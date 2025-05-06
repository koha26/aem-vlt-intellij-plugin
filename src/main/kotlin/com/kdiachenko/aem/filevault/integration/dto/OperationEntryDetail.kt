package com.kdiachenko.aem.filevault.integration.dto

/**
 * Detailed entry for a file operation
 */
data class OperationEntryDetail(
    val action: OperationAction,
    val path: String,
    val message: String? = ""
)
