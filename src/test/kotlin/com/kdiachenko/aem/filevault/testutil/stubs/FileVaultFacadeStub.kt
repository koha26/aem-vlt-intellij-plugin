package com.kdiachenko.aem.filevault.stubs

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.io.FileUtil
import com.kdiachenko.aem.filevault.integration.dto.DetailedOperationResult
import com.kdiachenko.aem.filevault.integration.facade.IFileVaultFacade
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterOperationOptions
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import java.io.File
import java.util.concurrent.CompletableFuture

open class FileVaultFacadeStub : IFileVaultFacade {
    val importedFiles = mutableListOf<String>()
    val exportedFiles = mutableListOf<String>()
    val importedOptions = mutableListOf<WorkspaceFilterOperationOptions>()
    val exportedOptions = mutableListOf<WorkspaceFilterOperationOptions>()

    override fun exportContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator,
        options: WorkspaceFilterOperationOptions,
    ): CompletableFuture<DetailedOperationResult> {
        return CompletableFuture.supplyAsync {
            exportedFiles.add(FileUtil.normalize(projectLocalFile.path))
            exportedOptions.add(options)
            return@supplyAsync DetailedOperationResult(true, "Exported", listOf())
        }
    }

    fun exportContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator,
    ): CompletableFuture<DetailedOperationResult> = exportContent(
        serverConfig = serverConfig,
        projectLocalFile = projectLocalFile,
        indicator = indicator,
        options = WorkspaceFilterOperationOptions(),
    )

    override fun importContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator,
        options: WorkspaceFilterOperationOptions,
    ): CompletableFuture<DetailedOperationResult> {
        return CompletableFuture.supplyAsync {
            importedFiles.add(FileUtil.normalize(projectLocalFile.path))
            importedOptions.add(options)
            return@supplyAsync DetailedOperationResult(true, "Imported", listOf())
        }
    }

    fun importContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator,
    ): CompletableFuture<DetailedOperationResult> = importContent(
        serverConfig = serverConfig,
        projectLocalFile = projectLocalFile,
        indicator = indicator,
        options = WorkspaceFilterOperationOptions(),
    )

}
