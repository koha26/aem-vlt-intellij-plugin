package com.kdiachenko.aem.filevault.service.dto

/**
 * Detailed entry for a file operation
 */
data class OperationEntryDetail(
    val action: OperationAction,
    val path: String,
    val message: String? = ""
)
