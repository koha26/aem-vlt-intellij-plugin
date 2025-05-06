package com.kdiachenko.aem.filevault.factory.impl

import com.kdiachenko.aem.filevault.factory.VaultAppFactory
import com.kdiachenko.aem.filevault.model.AEMServer
import com.kdiachenko.aem.filevault.service.CustomizedVaultFsApp
import org.apache.jackrabbit.vault.cli.VaultFsApp

class VaultAppFactoryImpl : VaultAppFactory {
    override fun createVaultApp(server: AEMServer): CustomizedVaultFsApp = CustomizedVaultFsApp(server)
}
