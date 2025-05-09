package com.kdiachenko.aem.filevault.integration.service

import com.intellij.openapi.progress.ProgressIndicator
import com.kdiachenko.aem.filevault.integration.dto.DetailedOperationResult
import com.kdiachenko.aem.filevault.model.AEMServerConfig
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import java.io.File
import java.util.concurrent.CompletableFuture

interface IFileVaultService {
    fun exportContent(
        serverConfig: DetailedAEMServerConfig,
        jcrPath: String,
        localPath: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult>

    fun importContent(
        serverConfig: DetailedAEMServerConfig,
        jcrPath: String,
        localPath: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult>
}
