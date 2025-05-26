package com.kdiachenko.aem.filevault.integration.dto

data class VltFilter(
    val root: String,
    val mode: String = "",
    val excludePatterns: List<String> = emptyList(),
    val includePatterns: List<String> = emptyList(),
)
