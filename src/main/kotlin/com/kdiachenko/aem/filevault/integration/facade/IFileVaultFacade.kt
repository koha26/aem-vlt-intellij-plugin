package com.kdiachenko.aem.filevault.integration.facade

import com.intellij.openapi.progress.ProgressIndicator
import com.kdiachenko.aem.filevault.integration.dto.DetailedOperationResult
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterOperationOptions
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import java.io.File
import java.util.concurrent.CompletableFuture

interface IFileVaultFacade {
    fun exportContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator,
        options: WorkspaceFilterOperationOptions = WorkspaceFilterOperationOptions(),
    ): CompletableFuture<DetailedOperationResult>

    fun importContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator,
        options: WorkspaceFilterOperationOptions = WorkspaceFilterOperationOptions(),
    ): CompletableFuture<DetailedOperationResult>
}
