package com.kdiachenko.aem.filevault.integration.service

import com.kdiachenko.aem.filevault.integration.dto.VltFilter
import java.nio.file.Path

/**
 * Interface for META-INF operations
 */
interface IMetaInfService {
    fun createFilterXml(tmpDir: Path, vltFilter: VltFilter)
}
