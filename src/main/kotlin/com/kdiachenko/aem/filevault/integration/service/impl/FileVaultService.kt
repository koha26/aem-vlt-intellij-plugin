package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.kdiachenko.aem.filevault.integration.dto.DetailedOperationResult
import com.kdiachenko.aem.filevault.integration.dto.OperationEntryDetail
import com.kdiachenko.aem.filevault.integration.dto.VltFilter
import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.factory.IVaultAppFactory
import com.kdiachenko.aem.filevault.integration.factory.impl.VaultAppFactory
import com.kdiachenko.aem.filevault.integration.listener.OperationProgressTrackerListener
import com.kdiachenko.aem.filevault.integration.service.*
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import com.kdiachenko.aem.filevault.util.JcrPathUtil.normalizeJcrPath
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toJcrPath
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
     * @param projectLocalFile Local path to export to
     * @param indicator Progress indicator
     * @return CompletableFuture with a detailed operation result
     */
    override fun exportContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult> = CompletableFuture.supplyAsync {
        val jcrPath = projectLocalFile.toJcrPath() ?: return@supplyAsync failed("Invalid JCR path.")
        val normalizedJcrPath = jcrPath.normalizeJcrPath()
        var tmpDir: Path? = null

        try {
            indicator.progress("Preparing export operation...", 0.1)
            tmpDir = setupExportOperation(normalizedJcrPath)

            indicator.progress("Exporting content from AEM...", 0.2)
            val progressTrackerListener = OperationProgressTrackerListener()
            executeVaultCommand(serverConfig, tmpDir, progressTrackerListener) {
                doExport(it)
            }
            indicator.progress("Processing exported content...", 0.7)
            val fileChangeTracker = processExportedContent(tmpDir, projectLocalFile, jcrPath)

            indicator.progress("Cleaning up...", 0.9)
            val result = createDetailedResult(
                "Successfully exported content to $jcrPath",
                progressTrackerListener.entries,
                fileChangeTracker.changes
            )

            return@supplyAsync result
        } catch (e: Exception) {
            logger.error("Error during content export", e)
            return@supplyAsync failed("Error: ${e.message}")
        } finally {
            cleanup(tmpDir)
            indicator.progress("", 1.0)
        }
    }

    private fun setupExportOperation(normalizedJcrPath: String): Path {
        val tmpDir = fileSystemService.createTempDirectory()
        metaInfService.createFilterXml(tmpDir, VltFilter(normalizedJcrPath))
        return tmpDir
    }

    private fun processExportedContent(tmpDir: Path, projectLocalFile: File, jcrPath: String): FileChangeTracker {
        val targetPath = projectLocalFile.toPath()
        val fileChangeTracker = FileChangeTracker()
        val exportedContentPath = tmpDir.resolve("$JCR_ROOT$jcrPath")

        when {
            Files.isDirectory(exportedContentPath) && Files.exists(exportedContentPath) -> {
                fileSystemService.copyDirectory(exportedContentPath, targetPath, fileChangeTracker)
                logger.info("Successfully copied content from $exportedContentPath to $targetPath")
            }

            Files.isRegularFile(exportedContentPath) && Files.exists(exportedContentPath) -> {
                fileSystemService.copyFile(exportedContentPath, targetPath, fileChangeTracker)
                logger.info("Successfully copied file from $exportedContentPath to $targetPath")
            }

            else -> logger.warn("No content was exported from $jcrPath")
        }

        return fileChangeTracker
    }

    private fun cleanup(tmpDir: Path?) {
        tmpDir?.let {
            fileSystemService.deleteDirectory(it)
        }
    }

    /**
     * Imports content from the local file system to AEM.
     *
     * @param serverConfig AEM server to import to
     * @param projectLocalFile Local path to import from
     * @param indicator Progress indicator
     * @return CompletableFuture with a detailed operation result
     */
    override fun importContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult> = CompletableFuture.supplyAsync {
        val jcrPath = projectLocalFile.toJcrPath() ?: return@supplyAsync failed("Invalid JCR path.")
        var tmpDir: Path? = null

        try {
            indicator.progress("Preparing import operation...", 0.1)
            tmpDir = prepareImportDirectory(jcrPath, projectLocalFile)

            indicator.progress("Preparing content for import...", 0.3)
            val fileChangeTracker = copyContentToImportDirectory(projectLocalFile, tmpDir, jcrPath)

            indicator.progress("Importing content to AEM...", 0.5)
            val progressTrackerListener = OperationProgressTrackerListener()
            executeVaultCommand(serverConfig, tmpDir, progressTrackerListener) {
                doImport(it)
            }

            indicator.progress("Cleaning up...", 0.9)
            val result = createDetailedResult(
                "Successfully imported content from $jcrPath",
                progressTrackerListener.entries,
                fileChangeTracker.changes
            )

            return@supplyAsync result
        } catch (e: Exception) {
            logger.error("Error during content import", e)
            return@supplyAsync failed("Error: ${e.message}")
        } finally {
            cleanup(tmpDir)
            indicator.progress("", 1.0)
        }
    }

    private fun prepareImportDirectory(
        jcrPath: String,
        projectLocalFile: File
    ): Path {
        val tmpDir = fileSystemService.createTempDirectory()
        val normalizedJcrPath = jcrPath.normalizeJcrPath()
        val filter = createFilterForImport(projectLocalFile, jcrPath, normalizedJcrPath)
        metaInfService.createFilterXml(tmpDir, filter)
        return tmpDir
    }

    private fun copyContentToImportDirectory(
        projectLocalFile: File,
        tmpDir: Path,
        jcrPath: String
    ): FileChangeTracker {
        val jcrRootPath = tmpDir.resolve(JCR_ROOT)
        val contentPath = jcrRootPath.resolve(jcrPath.substring(1))
        Files.createDirectories(contentPath)

        val sourcePath = projectLocalFile.toPath()
        return FileChangeTracker().apply {
            when {
                Files.isDirectory(sourcePath) -> {
                    fileSystemService.copyDirectory(sourcePath, contentPath, this)
                    logger.info("Copied directory from $sourcePath to $contentPath")
                }

                Files.isRegularFile(sourcePath) -> {
                    fileSystemService.copyFile(sourcePath, contentPath, this)
                    logger.info("Copied file from $sourcePath to $contentPath")
                }
            }
        }
    }

    private fun createFilterForImport(
        projectLocalFile: File,
        jcrPath: String,
        normalizedJcrPath: String
    ): VltFilter {
        if (jcrPath.endsWith("/.content.xml")) {
            val closestResources = projectLocalFile.parentFile.listFiles { it.name != ".content.xml" }
            val excludePatterns =
                closestResources.map { resource -> normalizedJcrPath + "/" + resource.name + "(/.*)?" }

            return VltFilter(root = normalizedJcrPath, excludePatterns = excludePatterns)
        }
        return VltFilter(normalizedJcrPath)
    }


    /**
     * Creates a detailed operation result by combining information from FileVault operations
     * and file change tracking.
     */
    private fun createDetailedResult(
        message: String,
        listenerEntries: List<OperationEntryDetail>,
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
            success = true,
            message = message,
            entries = processedEntries
        )
    }

    private fun failed(message: String): DetailedOperationResult = DetailedOperationResult(
        success = false,
        message = message,
        entries = emptyList()
    )

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

    private val fileSystemService: IFileSystemService = FileSystemService
    private val metaInfService: IMetaInfService = MetaInfService
    private val vaultAppFactory: IVaultAppFactory = VaultAppFactory

    private fun ProgressIndicator?.progress(text: String, fraction: Double) {
        this?.text = text
        this?.fraction = fraction
    }
}

