package com.kdiachenko.aem.filevault.integration.service

import java.nio.file.Path

/**
 * Interface for META-INF operations
 */
interface IMetaInfService {
    fun createFilterXml(tmpDir: Path, jcrPath: String)
}
