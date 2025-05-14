package com.kdiachenko.aem.filevault.integration.factory.impl

import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp
import com.kdiachenko.aem.filevault.integration.factory.IVaultAppFactory
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig

object VaultAppFactory : IVaultAppFactory {
    override fun createVaultApp(server: DetailedAEMServerConfig): CustomizedVaultFsApp = CustomizedVaultFsApp(server)
}
