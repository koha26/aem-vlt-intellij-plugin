package com.kdiachenko.aem.filevault.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.kdiachenko.aem.filevault.factory.VaultAppFactory
import com.kdiachenko.aem.filevault.factory.impl.VaultAppFactoryImpl
import com.kdiachenko.aem.filevault.model.AEMServer
import com.kdiachenko.aem.filevault.service.dto.DetailedOperationResult
import com.kdiachenko.aem.filevault.service.dto.OperationAction
import com.kdiachenko.aem.filevault.service.dto.OperationEntryDetail
import com.kdiachenko.aem.filevault.service.dto.VltBasicParams
import com.kdiachenko.aem.filevault.service.impl.FileSystemServiceImpl
import com.kdiachenko.aem.filevault.service.impl.MetaInfServiceImpl
import com.kdiachenko.aem.filevault.util.JcrPathUtil
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress
import org.apache.jackrabbit.vault.fs.io.*
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.absolutePathString

/**
 * Service for handling FileVault operations (push/pull)
 */
@Service(Service.Level.PROJECT)
class FileVaultService {
    private val logger = Logger.getInstance(FileVaultService::class.java)

    /**
     * Get JCR path from local file
     */
    fun getJcrPath(file: File): String {
        return JcrPathUtil.calculateJcrPath(file)
    }

    companion object {
        private const val META_INF_PATH = "META-INF"
        private const val FILTER_FILE_NAME = "filter.xml"
        private const val JCR_ROOT = "jcr_root"

        @JvmStatic
        fun getInstance(project: Project): FileVaultService {
            return project.getService(FileVaultService::class.java)
        }
    }

    /**
     * Exports content from AEM to the local file system.
     *
     * @param server AEM server to export from
     * @param jcrPath JCR path to export
     * @param localPath Local path to export to
     * @param indicator Progress indicator
     * @return CompletableFuture with detailed operation result
     */
    fun exportContent(
        server: AEMServer,
        jcrPath: String,
        localPath: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult> {
        return CompletableFuture.supplyAsync {
            try {
                val tmpDir = fileSystemService.createTempDirectory()
                logger.info("Created temporary directory at $tmpDir")

                // Update progress
                indicator.progress("Preparing export operation...", 0.1)

                // Create META-INF directory and filter.xml file
                metaInfService.createFilterXml(tmpDir, jcrPath)

                // Update progress
                indicator.progress("Exporting content from AEM...", 0.2)

                // Execute FileVault export
                val progressTrackerListener = OperationProgressTrackerListener()
                withPluginClassLoader {
                    executeVaultExport(server, tmpDir, progressTrackerListener)
                }

                // Update progress
                indicator.progress("Processing exported content...", 0.7)

                // Copy exported content to the selected path
                val exportedContentPath = tmpDir.resolve("$JCR_ROOT$jcrPath")
                val targetPath = localPath.toPath()

                val fileChangeTracker = FileChangeTracker()

                if (Files.exists(exportedContentPath)) {
                    fileSystemService.copyDirectory(exportedContentPath, targetPath, fileChangeTracker)
                    logger.info("Successfully copied content from $exportedContentPath to $targetPath")
                } else {
                    logger.warn("No content was exported from $jcrPath")
                }

                // Update progress
                indicator.progress("Cleaning up...", 0.9)

                // Clean up temp directory
                fileSystemService.deleteDirectory(tmpDir)

                indicator.progress("", 1.0)

                // Process the operation entries from both listener and file tracking
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
     * @return CompletableFuture with detailed operation result
     */
    fun importContent(
        server: AEMServer,
        jcrPath: String,
        localPath: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult> {
        return CompletableFuture.supplyAsync {
            try {
                val tmpDir = fileSystemService.createTempDirectory()
                logger.info("Created temporary directory at $tmpDir")

                // Update progress
                indicator.progress( "Preparing import operation...", 0.1)

                // Create META-INF directory and filter.xml file
                metaInfService.createFilterXml(tmpDir, jcrPath)

                // Update progress
                indicator.progress( "Preparing content for import...", 0.3)

                // Create JCR root directory structure
                val jcrRootPath = tmpDir.resolve(JCR_ROOT)
                val contentPath = jcrRootPath.resolve(jcrPath.substring(1)) // Remove leading slash
                Files.createDirectories(contentPath)

                // Copy content from selected path to temp directory
                val sourcePath = localPath.toPath()
                val fileChangeTracker = FileChangeTracker()
                fileSystemService.copyDirectory(sourcePath, contentPath, fileChangeTracker)
                logger.info("Copied content from $sourcePath to $contentPath")

                // Update progress
                indicator.progress( "Importing content to AEM...", 0.5)
                // Execute FileVault import command
                val progressTrackerListener = OperationProgressTrackerListener()
                withPluginClassLoader {
                    executeVaultImport(server, tmpDir, progressTrackerListener)
                }

                // Update progress
                indicator.progress( "Cleaning up...", 0.9)

                // Clean up temp directory
                fileSystemService.deleteDirectory(tmpDir)
                indicator.progress( "", 1.0)

                // Process the operation entries from both listener and file tracking
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
        // Merge and process entries from both sources
        val processedEntries = mutableListOf<OperationEntryDetail>()

        // Process listener entries first
        listenerEntries.forEach { entry ->
            val action = when {
                entry.action.contains("A") -> OperationAction.ADDED
                entry.action.contains("U") -> OperationAction.UPDATED
                entry.action.contains("D") -> OperationAction.DELETED
                else -> OperationAction.NOT_TOUCHED
            }

            processedEntries.add(
                OperationEntryDetail(
                    action = action,
                    path = entry.path,
                    message = entry.message
                )
            )
        }

        // Add file change entries that aren't already covered
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

    /**
     * Executes the FileVault export command.
     */
    private fun executeVaultExport(
        server: AEMServer,
        tmpDir: Path,
        progressListener: OperationProgressTrackerListener
    ) {
        val vaultFsApp = vaultAppFactory.createVaultApp(server)
        vaultFsApp.init()
        doExport(
            vaultFsApp,
            VltBasicParams(
                jcrPath = "/",
                localPath = tmpDir.absolutePathString(),
                mountPoint = server.url + "/crx"
            ),
            progressListener
        )
    }

    private fun doExport(
        app: CustomizedVaultFsApp,
        params: VltBasicParams,
        progressListener: OperationProgressTrackerListener
    ) {
        val verbose = true
        val jcrPath = params.jcrPath
        val localPath = params.localPath
        val addr = RepositoryAddress(params.mountPoint)
        val localFile = app.getPlatformFile(localPath, false)

        var exporter: AbstractExporter? = null
        try {
            if (!localFile.exists()) {
                localFile.mkdirs()
            }
            exporter = PlatformExporter(localFile)
            val vCtx = app.createVaultContext(localFile)

            vCtx.isVerbose = verbose
            val vaultFile = vCtx.getFileSystem(addr).getFile(jcrPath)
            if (vaultFile == null) {
                logger.error("Not such remote file: $jcrPath")
                return
            }

            logger.info("Exporting ${vaultFile.path} to ${localFile.canonicalPath}")
            if (verbose) {
                exporter.setVerbose(progressListener)
            }
            exporter.isNoMetaInf = true
            exporter.export(vaultFile)
            logger.info("Exporting done.")
        } finally {
            exporter?.close()
        }
    }

    private fun doImport(
        app: CustomizedVaultFsApp,
        params: VltBasicParams,
        progressListener: OperationProgressTrackerListener
    ) {
        val verbose = true
        val jcrPath = params.jcrPath
        val localPath = params.localPath
        val addr = RepositoryAddress(params.mountPoint)
        val localFile = app.getPlatformFile(localPath, false)
        val vCtx = app.createVaultContext(localFile)
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
                importer.options.listener = progressListener
            }
            val session = vaultFile.fileSystem.aggregateManager.session
            importer.run(archive, session, vaultFile.path)
            logger.info("Importing done.")
        } finally {
            archive?.close()
        }
    }

    /**
     * Executes the FileVault import command.
     */
    private fun executeVaultImport(
        server: AEMServer,
        tmpDir: Path,
        progressListener: OperationProgressTrackerListener
    ) {
        val vaultFsApp = vaultAppFactory.createVaultApp(server)
        vaultFsApp.init()
        doImport(
            vaultFsApp,
            VltBasicParams(
                jcrPath = "/",
                localPath = tmpDir.absolutePathString(),
                mountPoint = server.url + "/crx"
            ),
            progressListener
        )
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

    // Inject dependencies
    private val fileSystemService: FileSystemService = FileSystemServiceImpl()
    private val metaInfService: MetaInfService = MetaInfServiceImpl()
    private val vaultAppFactory: VaultAppFactory = VaultAppFactoryImpl()

    private fun ProgressIndicator.progress(text: String, fraction: Double) {
        this.text = text
        this.fraction = fraction
    }
}
