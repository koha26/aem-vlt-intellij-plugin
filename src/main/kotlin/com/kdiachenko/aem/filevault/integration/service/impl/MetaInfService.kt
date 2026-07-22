package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.openapi.components.service
import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * Implementation of MetaInfService
 */
class MetaInfService : IMetaInfService {

    companion object {
        @JvmStatic
        fun getInstance(): IMetaInfService {
            return service()
        }
    }

    override fun createFilterXml(tmpDir: Path, workspaceFilter: WorkspaceFilter) {
        val filterFile = tmpDir.resolve("META-INF/vault/filter.xml")
        Files.createDirectories(filterFile.parent)
        workspaceFilter.source.use { source ->
            Files.copy(source, filterFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
