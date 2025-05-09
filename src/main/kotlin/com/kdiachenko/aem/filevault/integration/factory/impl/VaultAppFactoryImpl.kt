package com.kdiachenko.aem.filevault.integration.factory.impl

import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp
import com.kdiachenko.aem.filevault.integration.factory.VaultAppFactory
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig

class VaultAppFactoryImpl : VaultAppFactory {
    override fun createVaultApp(server: DetailedAEMServerConfig): CustomizedVaultFsApp = CustomizedVaultFsApp(server)
}
