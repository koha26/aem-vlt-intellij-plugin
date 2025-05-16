package com.kdiachenko.aem.filevault.integration.factory.impl

import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp
import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.factory.IVaultAppFactory
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig

object VaultAppFactory : IVaultAppFactory {
    override fun createVaultApp(context: VltOperationContext): CustomizedVaultFsApp =
        CustomizedVaultFsApp(context)
}
