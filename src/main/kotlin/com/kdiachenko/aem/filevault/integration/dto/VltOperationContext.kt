package com.kdiachenko.aem.filevault.integration.dto

import com.kdiachenko.aem.filevault.integration.listener.OperationProgressTrackerListener
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig

data class VltOperationContext(
    val jcrPath: String = "/",
    val localAbsPath: String,
    val serverConfig: DetailedAEMServerConfig,
    val progressListener: OperationProgressTrackerListener? = null
)
