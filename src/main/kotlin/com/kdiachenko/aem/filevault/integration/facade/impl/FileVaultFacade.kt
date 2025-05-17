package com.kdiachenko.aem.filevault.integration.facade.impl

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.kdiachenko.aem.filevault.integration.dto.*
import com.kdiachenko.aem.filevault.integration.facade.IFileVaultFacade
import com.kdiachenko.aem.filevault.integration.listener.OperationProgressTrackerListener
import com.kdiachenko.aem.filevault.integration.service.*
import com.kdiachenko.aem.filevault.integration.service.impl.FileSystemService
import com.kdiachenko.aem.filevault.integration.service.impl.MetaInfService
import com.kdiachenko.aem.filevault.integration.service.impl.VaultOperationService
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import com.kdiachenko.aem.filevault.util.JcrPathUtil.normalizeJcrPath
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toJcrPath
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.io.path.absolutePathString

/**
 * Service for handling FileVault operations (push/pull)
 */
@Service(Service.Level.PROJECT)
class FileVaultFacade : IFileVaultFacade {
    private val logger = Logger.getInstance(FileVaultFacade::class.java)

    companion object {
        private const val JCR_ROOT = "jcr_root"

        @JvmStatic
        fun getInstance(project: Project): FileVaultFacade {
            return project.getService(FileVaultFacade::class.java)
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
            vaultOperationService.export(
                VltOperationContext(
                    serverConfig = serverConfig,
                    localAbsPath = tmpDir.absolutePathString(),
                    progressListener = progressTrackerListener,
                )
            )
            indicator.progress("Processing exported content...", 0.7)
            val fileChangeTracker = processExportedContent(tmpDir, projectLocalFile, jcrPath)

            indicator.progress("Cleaning up...", 0.9)
            val operationEntryDetails = fileChangeTracker.changes.map {
                OperationEntryDetail(
                    action = it.action,
                    path = it.path,
                    message = it.reason
                )
            }.filterOutNothingChanged()
            val result = DetailedOperationResult(
                success = true,
                message = buildString {
                    appendLine("Successfully exported content to $jcrPath")
                    if (operationEntryDetails.isNotEmpty()) {
                        appendLine("<br/>Files changes statistic:")
                    } else {
                        appendLine("<br/>Files were not changed.")
                    }
                    appendLine(createDetailedResult(operationEntryDetails))
                },
                entries = operationEntryDetails
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
            copyContentToImportDirectory(projectLocalFile, tmpDir, jcrPath)

            indicator.progress("Importing content to AEM...", 0.5)
            val progressTrackerListener = OperationProgressTrackerListener()
            vaultOperationService.import(
                VltOperationContext(
                    serverConfig = serverConfig,
                    localAbsPath = tmpDir.absolutePathString(),
                    progressListener = progressTrackerListener,
                )
            )

            indicator.progress("Cleaning up...", 0.9)

            val operationEntryDetails = progressTrackerListener.entries.filterOutNothingChanged()
            val result = DetailedOperationResult(
                success = true,
                message = buildString {
                    appendLine("Successfully imported content from $jcrPath.")
                    if (operationEntryDetails.isNotEmpty()) {
                        appendLine("<br/>Nodes changes statistic:")
                    } else {
                        appendLine("<br/>Nodes were not changed.")
                    }
                    appendLine(createDetailedResult(operationEntryDetails))
                },
                entries = operationEntryDetails
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

    private fun List<OperationEntryDetail>.filterOutNothingChanged(): List<OperationEntryDetail> =
        this.filter { OperationAction.NOTHING_CHANGED != it.action }

    private fun createDetailedResult(operationEntries: List<OperationEntryDetail>): String {
        val updated = operationEntries.filter { OperationAction.UPDATED == it.action }
        val removed = operationEntries.filter { OperationAction.DELETED == it.action }
        val added = operationEntries.filter { OperationAction.ADDED == it.action }
        val error = operationEntries.filter { OperationAction.ERROR == it.action }

        return buildString {
            if (updated.isNotEmpty()) {
                append("Updated: ${updated.size}")
            }
            if (added.isNotEmpty()) {
                append(" | Added: ${added.size}")
            }
            if (removed.isNotEmpty()) {
                append(" | Removed: ${removed.size}")
            }
            if (error.isNotEmpty()) {
                append(" | Error: ${error.size}")
            }
        }
    }

    private fun failed(message: String): DetailedOperationResult = DetailedOperationResult(
        success = false,
        message = message,
        entries = emptyList()
    )


    private val fileSystemService: IFileSystemService = FileSystemService
    private val metaInfService: IMetaInfService = MetaInfService
    private val vaultOperationService: IVaultOperationService = VaultOperationService

    private fun ProgressIndicator?.progress(text: String, fraction: Double) {
        this?.text = text
        this?.fraction = fraction
    }
}
