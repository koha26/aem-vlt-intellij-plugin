package com.kdiachenko.aem.filevault.integration.service

import java.nio.file.Path

/**
 * Interface for META-INF operations
 */
interface IMetaInfService {
    fun createFilterXml(tmpDir: Path, filter: Filter)
}

data class Filter(
    val root: String,
    val mode: String = "",
    val excludePatterns: List<String> = emptyList(),
    val includePatterns: List<String> = emptyList(),
) {}
