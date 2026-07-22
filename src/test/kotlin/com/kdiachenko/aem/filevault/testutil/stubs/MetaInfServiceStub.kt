package com.kdiachenko.aem.filevault.stubs

import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter
import java.nio.file.Path

class MetaInfServiceStub : IMetaInfService {
    val workspaceFilters = mutableListOf<Pair<Path, WorkspaceFilter>>()

    override fun createFilterXml(tmpDir: Path, workspaceFilter: WorkspaceFilter) {
        workspaceFilters.add(tmpDir to workspaceFilter)
    }
}
