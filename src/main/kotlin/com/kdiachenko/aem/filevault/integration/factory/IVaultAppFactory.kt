package com.kdiachenko.aem.filevault.integration.factory

import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp
import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext

/**
 * Factory for creating VaultFsApp instances
 */
interface IVaultAppFactory {
    fun createVaultApp(context: VltOperationContext): CustomizedVaultFsApp
}
