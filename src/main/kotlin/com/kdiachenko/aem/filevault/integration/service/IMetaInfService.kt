package com.kdiachenko.aem.filevault.integration.service

import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter
import java.nio.file.Path

/**
 * Interface for META-INF operations
 */
interface IMetaInfService {
    fun createFilterXml(tmpDir: Path, workspaceFilter: WorkspaceFilter)
}
