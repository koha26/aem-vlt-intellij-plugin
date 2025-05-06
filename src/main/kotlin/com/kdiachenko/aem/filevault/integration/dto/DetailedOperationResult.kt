package com.kdiachenko.aem.filevault.integration.dto

/**
 * Detailed result of a FileVault operation
 */
data class DetailedOperationResult(
    val success: Boolean,
    val message: String,
    val entries: List<OperationEntryDetail>
)
