package com.kdiachenko.aem.filevault.integration.factory

import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig

/**
 * Factory for creating VaultFsApp instances
 */
interface IVaultAppFactory {
    fun createVaultApp(server: DetailedAEMServerConfig): CustomizedVaultFsApp
}
