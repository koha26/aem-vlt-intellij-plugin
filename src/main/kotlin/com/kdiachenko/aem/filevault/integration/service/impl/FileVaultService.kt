package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.kdiachenko.aem.filevault.integration.dto.DetailedOperationResult
import com.kdiachenko.aem.filevault.integration.dto.OperationEntryDetail
import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.factory.VaultAppFactory
import com.kdiachenko.aem.filevault.integration.factory.impl.VaultAppFactoryImpl
import com.kdiachenko.aem.filevault.integration.service.*
import com.kdiachenko.aem.filevault.model.AEMServerConfig
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress
import org.apache.jackrabbit.vault.fs.io.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.absolutePathString

/**
 * Service for handling FileVault operations (push/pull)
 */
@Service(Service.Level.PROJECT)
class FileVaultService : IFileVaultService {
    private val logger = Logger.getInstance(FileVaultService::class.java)

    companion object {
        private const val JCR_ROOT = "jcr_root"

        @JvmStatic
        fun getInstance(project: Project): FileVaultService {
            return project.getService(FileVaultService::class.java)
        }
    }

    /**
     * Exports content from AEM to the local file system.
     *
     * @param serverConfig AEM server to export from
     * @param jcrPath JCR path to export
     * @param localPath Local path to export to
     * @param indicator Progress indicator
     * @return CompletableFuture with a detailed operation result
     */
    override fun exportContent(
        serverConfig: DetailedAEMServerConfig,
        jcrPath: String,
        localPath: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult> = CompletableFuture.supplyAsync {
        var tmpDir: Path? = null
        try {
            tmpDir = fileSystemService.createTempDirectory()
            indicator.progress("Preparing export operation...", 0.1)

            metaInfService.createFilterXml(tmpDir, jcrPath)
            indicator.progress("Exporting content from AEM...", 0.2)

            val progressTrackerListener = OperationProgressTrackerListener()
            executeVaultCommand(serverConfig, tmpDir, progressTrackerListener) {
                doExport(it)
            }
            indicator.progress("Processing exported content...", 0.7)

            val exportedContentPath = tmpDir.resolve("$JCR_ROOT$jcrPath")
            val targetPath = localPath.toPath()
            val fileChangeTracker = FileChangeTracker()

            if (Files.exists(exportedContentPath)) {
                fileSystemService.copyDirectory(exportedContentPath, targetPath, fileChangeTracker)
                logger.info("Successfully copied content from $exportedContentPath to $targetPath")
            } else {
                logger.warn("No content was exported from $jcrPath")
            }
            indicator.progress("Cleaning up...", 0.9)

            val result = createDetailedResult(
                true,
                "Successfully exported content to $localPath",
                progressTrackerListener.entries,
                fileChangeTracker.changes
            )

            return@supplyAsync result
        } catch (e: Exception) {
            logger.error("Error during content export", e)
            return@supplyAsync DetailedOperationResult(
                success = false,
                message = "Error: ${e.message}",
                entries = emptyList()
            )
        } finally {
            tmpDir?.let {
                fileSystemService.deleteDirectory(it)
                indicator.progress("", 1.0)
            }
        }
    }

    /**
     * Imports content from the local file system to AEM.
     *
     * @param server AEM server to import to
     * @param jcrPath JCR path to import to
     * @param localPath Local path to import from
     * @param indicator Progress indicator
     * @return CompletableFuture with a detailed operation result
     */
    override fun importContent(
        serverConfig: DetailedAEMServerConfig,
        jcrPath: String,
        localPath: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult> = CompletableFuture.supplyAsync {
        var tmpDir: Path? = null
        try {
            tmpDir = fileSystemService.createTempDirectory()
            indicator.progress("Preparing import operation...", 0.1)

            metaInfService.createFilterXml(tmpDir, jcrPath)
            indicator.progress("Preparing content for import...", 0.3)

            val jcrRootPath = tmpDir.resolve(JCR_ROOT) // Create JCR root directory structure
            val contentPath = jcrRootPath.resolve(jcrPath.substring(1))
            Files.createDirectories(contentPath)

            val sourcePath = localPath.toPath()
            val fileChangeTracker = FileChangeTracker()
            fileSystemService.copyDirectory(sourcePath, contentPath, fileChangeTracker)
            logger.info("Copied content from $sourcePath to $contentPath")
            indicator.progress("Importing content to AEM...", 0.5)

            val progressTrackerListener = OperationProgressTrackerListener()
            executeVaultCommand(serverConfig, tmpDir, progressTrackerListener) {
                doImport(it)
            }
            indicator.progress("Cleaning up...", 0.9)

            val result = createDetailedResult(
                true,
                "Successfully imported content from $localPath",
                progressTrackerListener.entries,
                fileChangeTracker.changes
            )

            return@supplyAsync result
        } catch (e: Exception) {
            logger.error("Error during content import", e)
            return@supplyAsync DetailedOperationResult(
                success = false,
                message = "Error: ${e.message}",
                entries = emptyList()
            )
        } finally {
            tmpDir?.let {
                fileSystemService.deleteDirectory(it)
                indicator.progress("", 1.0)
            }
        }
    }

    /**
     * Creates a detailed operation result by combining information from FileVault operations
     * and file change tracking.
     */
    private fun createDetailedResult(
        success: Boolean,
        message: String,
        listenerEntries: List<OperationEntry>,
        fileChanges: List<FileChangeEntry>
    ): DetailedOperationResult {
        val processedEntries = mutableListOf<OperationEntryDetail>()

        listenerEntries.forEach { entry ->
            processedEntries.add(
                OperationEntryDetail(
                    action = entry.action,
                    path = entry.path,
                    message = entry.message
                )
            )
        }

        fileChanges.forEach { change ->
            val existingEntry = processedEntries.find { it.path == change.path }
            if (existingEntry == null) {
                processedEntries.add(
                    OperationEntryDetail(
                        action = change.action,
                        path = change.path,
                        message = change.reason
                    )
                )
            }
        }

        return DetailedOperationResult(
            success = success,
            message = message,
            entries = processedEntries
        )
    }

    private fun doExport(context: VltOperationContext) {
        val verbose = true
        val jcrPath = context.jcrPath
        val localPath = context.localPath
        val addr = RepositoryAddress(context.mountPointUrl)
        val localFile = context.vaultFsApp.getPlatformFile(localPath, false)

        var exporter: AbstractExporter? = null
        try {
            if (!localFile.exists()) {
                localFile.mkdirs()
            }
            exporter = PlatformExporter(localFile)
            val vCtx = context.vaultFsApp.createVaultContext(localFile)

            vCtx.isVerbose = verbose
            val vaultFile = vCtx.getFileSystem(addr).getFile(jcrPath)
            if (vaultFile == null) {
                logger.error("Not such remote file: $jcrPath")
                return
            }

            logger.info("Exporting ${vaultFile.path} to ${localFile.canonicalPath}")
            if (verbose) {
                exporter.setVerbose(context.progressListener)
            }
            exporter.isNoMetaInf = true
            exporter.export(vaultFile)
            logger.info("Exporting done.")
        } finally {
            exporter?.close()
        }
    }


    private fun doImport(context: VltOperationContext) {
        val verbose = true
        val jcrPath = context.jcrPath
        val localPath = context.localPath
        val addr = RepositoryAddress(context.mountPointUrl)
        val localFile = context.vaultFsApp.getPlatformFile(localPath, false)
        val vCtx = context.vaultFsApp.createVaultContext(localFile)
        vCtx.isVerbose = verbose
        val vaultFile = vCtx.getFileSystem(addr).getFile(jcrPath)
        logger.info("Importing ${localFile.canonicalPath} to ${vaultFile.path}")

        var archive: Archive? = null
        try {
            if (!localFile.exists()) {
                localFile.mkdirs()
            }
            archive = FileArchive(localFile)
            archive.open(false)
            val importer = Importer()
            if (verbose) {
                importer.options.listener = context.progressListener
            }
            val session = vaultFile.fileSystem.aggregateManager.session
            importer.run(archive, session, vaultFile.path)
            logger.info("Importing done.")
        } finally {
            archive?.close()
        }
    }

    private fun executeVaultCommand(
        serverConfig: DetailedAEMServerConfig,
        tmpDir: Path,
        progressListener: OperationProgressTrackerListener,
        operation: (VltOperationContext) -> Unit
    ) {
        withPluginClassLoader {
            val vaultFsApp = vaultAppFactory.createVaultApp(serverConfig)
            vaultFsApp.init()
            operation(
                VltOperationContext(
                    vaultFsApp = vaultFsApp,
                    jcrPath = "/",
                    localPath = tmpDir.absolutePathString(),
                    mountPointUrl = serverConfig.url + "/crx",
                    progressListener = progressListener
                )
            )
        }
    }

    private fun withPluginClassLoader(callback: () -> Unit) {
        val currentThread = Thread.currentThread()
        val originalClassLoader = currentThread.contextClassLoader
        val pluginClassLoader = this.javaClass.classLoader
        try {
            currentThread.contextClassLoader = pluginClassLoader
            callback()
        } finally {
            currentThread.contextClassLoader = originalClassLoader
        }
    }

    private val fileSystemService: IFileSystemService = FileSystemService()
    private val metaInfService: IMetaInfService = MetaInfService()
    private val vaultAppFactory: VaultAppFactory = VaultAppFactoryImpl()

    private fun ProgressIndicator?.progress(text: String, fraction: Double) {
        this?.text = text
        this?.fraction = fraction
    }
}
