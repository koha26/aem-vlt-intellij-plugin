package com.kdiachenko.aem.filevault.integration.dto

import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp
import com.kdiachenko.aem.filevault.integration.service.OperationProgressTrackerListener

data class VltOperationContext(
    val vaultFsApp: CustomizedVaultFsApp,
    val jcrPath: String,
    val localPath: String,
    val mountPointUrl: String,
    val progressListener: OperationProgressTrackerListener? = null
)
