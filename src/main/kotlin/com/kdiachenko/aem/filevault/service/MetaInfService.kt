package com.kdiachenko.aem.filevault.service

import java.nio.file.Path

/**
 * Interface for META-INF operations
 */
interface MetaInfService {
    fun createFilterXml(tmpDir: Path, jcrPath: String)
}
