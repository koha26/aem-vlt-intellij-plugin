package com.kdiachenko.aem.filevault.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.kdiachenko.aem.filevault.model.AEMServer
import com.kdiachenko.aem.filevault.service.dto.VltBasicParams
import com.kdiachenko.aem.filevault.util.JcrPathUtil
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
class FileVaultService() {
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
     * @param selectedPath Selected path in the project
     * @param jcrPath JCR path to export (usually derived from selectedPath)
     * @return CompletableFuture that completes when the export operation is done
     */
    fun exportContent(
        server: AEMServer,
        jcrPath: String,
        localPath: File,
        indicator: ProgressIndicator
    ): CompletableFuture<OperationResult> {
        return CompletableFuture.supplyAsync {
            try {
                val tmpDir = createTempDirectory()
                logger.info("Created temporary directory at $tmpDir")

                // Create META-INF directory and filter.xml file
                createFilterXml(tmpDir, jcrPath)

                withPluginClassLoader {
                    executeVaultExport(server, tmpDir)
                }

                // Copy exported content to the selected path
                val exportedContentPath = tmpDir.resolve("$JCR_ROOT$jcrPath")
                val targetPath = localPath.toPath()

                if (Files.exists(exportedContentPath)) {
                    copyDirectory(exportedContentPath, targetPath)
                    logger.info("Successfully copied content from $exportedContentPath to $targetPath")
                } else {
                    logger.warn("No content was exported from $jcrPath")
                }

                // Clean up temp directory
                deleteDirectory(tmpDir)

                return@supplyAsync OperationResult(true, "Successfully exported content to $localPath")
            } catch (e: Exception) {
                logger.error("Error during content export", e)
                //throw RuntimeException("Failed to export content", e)
                return@supplyAsync OperationResult(false, "Error: ${e.message}")
            }
        }
    }

    /**
     * Imports content from the local file system to AEM.
     *
     * @param selectedPath Selected path in the project
     * @param jcrPath JCR path to import to (usually derived from selectedPath)
     * @return CompletableFuture that completes when the import operation is done
     */
    fun importContent(
        server: AEMServer,
        jcrPath: String,
        localPath: File,
        indicator: ProgressIndicator
    ): CompletableFuture<OperationResult> {
        return CompletableFuture.supplyAsync {
            try {
                val tmpDir = createTempDirectory()

                logger.info("Created temporary directory at $tmpDir")

                // Create META-INF directory and filter.xml file
                createFilterXml(tmpDir, jcrPath)

                // Create JCR root directory structure
                val jcrRootPath = tmpDir.resolve(JCR_ROOT)
                val contentPath = jcrRootPath.resolve(jcrPath.substring(1)) // Remove leading slash
                Files.createDirectories(contentPath)

                // Copy content from selected path to temp directory
                val sourcePath = localPath.toPath()
                copyDirectory(sourcePath, contentPath)
                logger.info("Copied content from $sourcePath to $contentPath")

                // Execute FileVault import command
                withPluginClassLoader {
                    executeVaultImport(server, tmpDir)
                }

                // Clean up temp directory
                deleteDirectory(tmpDir)
                return@supplyAsync OperationResult(true, "Successfully imported content from $localPath")
            } catch (e: Exception) {
                logger.error("Error during content import", e)
                //throw RuntimeException("Failed to import content", e)
                return@supplyAsync OperationResult(false, "Error: ${e.message}")
            }
        }
    }

    /**
     * Creates a temporary directory for FileVault operations.
     *
     * @return Path to the created temporary directory
     * @throws IOException If directory creation fails
     */
    @Throws(IOException::class)
    private fun createTempDirectory(): Path {
        return kotlin.io.path.createTempDirectory("aem-filevault-pull-${UUID.randomUUID()}")
    }

    /**
     * Creates the META-INF/filter.xml file for FileVault operations.
     *
     * @param tmpDir Temporary directory
     * @param jcrPath JCR path to include in the filter
     * @throws IOException If file creation fails
     */
    @Throws(IOException::class)
    private fun createFilterXml(tmpDir: Path, jcrPath: String) {
        val metaInfDir = tmpDir.resolve(META_INF_PATH + "/vault")
        Files.createDirectories(metaInfDir)

        val filterFile = metaInfDir.resolve(FILTER_FILE_NAME)

        val filterContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <workspaceFilter version="1.0">
                <filter root="$jcrPath"/>
            </workspaceFilter>
        """.trimIndent()

        Files.write(filterFile, filterContent.toByteArray())
        logger.info("Created filter.xml at $filterFile with content:\n$filterContent")
    }

    /**
     * Executes the FileVault export command.
     */
    private fun executeVaultExport(server: AEMServer, tmpDir: Path) {
        val vaultFsApp = CustomizedVaultFsApp(server)
        vaultFsApp.init()
        doExport(
            vaultFsApp, VltBasicParams(
                jcrPath = "/",
                localPath = tmpDir.absolutePathString(),
                mountPoint = server.url + "/crx"
            )
        )
    }

    private fun doExport(app: CustomizedVaultFsApp, params: VltBasicParams) {
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

            logger.info("Exporting ${vaultFile.path} to ${localFile.getCanonicalPath()}")
            if (verbose) {
                exporter.setVerbose(OperationProgressTrackerListener())
            }
            exporter.isNoMetaInf = true
            exporter.export(vaultFile)
            logger.info("Exporting done.")
            exporter.exportInfo.entries.forEach {
                logger.info("Exported: ${it.key} => ${it.value}")
            }
        } finally {
            exporter?.close()
        }
    }

    private fun doImport(app: CustomizedVaultFsApp, params: VltBasicParams) {
        val verbose = true
        val jcrPath = params.jcrPath
        val localPath = params.localPath
        val addr = RepositoryAddress(params.mountPoint)
        val localFile = app.getPlatformFile(localPath, false)
        val vCtx = app.createVaultContext(localFile)
        vCtx.isVerbose = verbose
        val vaultFile = vCtx.getFileSystem(addr).getFile(jcrPath)
        logger.info("Importing ${localFile.getCanonicalPath()} to ${vaultFile.path}")

        var archive: Archive? = null
        try {
            if (!localFile.exists()) {
                localFile.mkdirs()
            }
            archive = FileArchive(localFile)
            archive.open(false)
            val importer = Importer()
            if (verbose) {
                importer.options.listener = OperationProgressTrackerListener()
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
    private fun executeVaultImport(server: AEMServer, tmpDir: Path) {
        val vaultFsApp = CustomizedVaultFsApp(server)
        vaultFsApp.init()
        doImport(
            vaultFsApp, VltBasicParams(
                jcrPath = "/",
                localPath = tmpDir.absolutePathString(),
                mountPoint = server.url + "/crx"
            )
        )
    }

    private fun withPluginClassLoader(callback: () -> Unit) {
        val currentThread = Thread.currentThread()
        val originalClassLoader = currentThread.getContextClassLoader()
        val pluginClassLoader = this.javaClass.getClassLoader()
        try {
            currentThread.setContextClassLoader(pluginClassLoader)
            callback()
        } finally {
            currentThread.setContextClassLoader(originalClassLoader)
        }
    }

    /**
     * Copies a directory recursively.
     *
     * @param source Source directory
     * @param target Target directory
     * @throws IOException If copying fails
     */
    @Throws(IOException::class)
    private fun copyDirectory(source: Path, target: Path) {
        Files.walkFileTree(source, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val targetDir = target.resolve(source.relativize(dir))
                Files.createDirectories(targetDir)
                return FileVisitResult.CONTINUE
            }

            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING)
                return FileVisitResult.CONTINUE
            }
        })
    }

    /**
     * Deletes a directory recursively.
     *
     * @param directory Directory to delete
     * @throws IOException If deletion fails
     */
    @Throws(IOException::class)
    private fun deleteDirectory(directory: Path?) {
        if (directory == null || !Files.exists(directory)) {
            return
        }

        Files.walkFileTree(directory, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            @Throws(IOException::class)
            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })

        logger.info("Deleted temporary directory: $directory")
    }

    /**
     * Result of a FileVault operation
     */
    data class OperationResult(val success: Boolean, val message: String)
}
