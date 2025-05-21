package com.kdiachenko.aem.filevault.integration.factory.impl

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp
import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.factory.IVaultAppFactory
import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig

class VaultAppFactory : IVaultAppFactory {

    companion object {
        @JvmStatic
        fun getInstance(): IVaultAppFactory {
            return service()
        }
    }

    override fun createVaultApp(context: VltOperationContext): CustomizedVaultFsApp =
        CustomizedVaultFsApp(context)
}
