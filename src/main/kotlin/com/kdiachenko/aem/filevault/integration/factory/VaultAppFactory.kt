package com.kdiachenko.aem.filevault.integration.factory

import com.kdiachenko.aem.filevault.model.AEMServer
import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp

/**
 * Factory for creating VaultFsApp instances
 */
interface VaultAppFactory {
    fun createVaultApp(server: AEMServer): CustomizedVaultFsApp
}
