package com.kdiachenko.aem.filevault.integration.factory.impl

import com.kdiachenko.aem.filevault.integration.factory.VaultAppFactory
import com.kdiachenko.aem.filevault.model.AEMServer
import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp

class VaultAppFactoryImpl : VaultAppFactory {
    override fun createVaultApp(server: AEMServer): CustomizedVaultFsApp = CustomizedVaultFsApp(server)
}
