package com.kdiachenko.aem.filevault.integration.facade.impl

import com.intellij.openapi.progress.ProgressIndicator
import com.kdiachenko.aem.filevault.integration.dto.DetailedOperationResult
import com.kdiachenko.aem.filevault.integration.facade.IFileVaultFacade
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import java.io.File
import java.util.concurrent.CompletableFuture

class FileVaultFacadeStub : IFileVaultFacade {
    val importedFiles = mutableListOf<String>()
    val exportedFiles = mutableListOf<String>()

    override fun exportContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult> {
        return CompletableFuture.supplyAsync {
            exportedFiles.add(projectLocalFile.path)
            return@supplyAsync DetailedOperationResult(true, "Exported", listOf())
        }
    }

    override fun importContent(
        serverConfig: DetailedAEMServerConfig,
        projectLocalFile: File,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailedOperationResult> {
        return CompletableFuture.supplyAsync {
            importedFiles.add(projectLocalFile.path)
            return@supplyAsync DetailedOperationResult(true, "Imported", listOf())
        }
    }

}
