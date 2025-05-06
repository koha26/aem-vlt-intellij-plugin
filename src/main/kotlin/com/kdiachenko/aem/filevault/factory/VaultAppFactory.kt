package com.kdiachenko.aem.filevault.factory

import com.kdiachenko.aem.filevault.model.AEMServer
import com.kdiachenko.aem.filevault.service.CustomizedVaultFsApp

/**
 * Factory for creating VaultFsApp instances
 */
interface VaultAppFactory {
    fun createVaultApp(server: AEMServer): CustomizedVaultFsApp
}
