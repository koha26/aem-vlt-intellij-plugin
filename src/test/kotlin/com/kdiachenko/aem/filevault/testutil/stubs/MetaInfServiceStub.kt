package com.kdiachenko.aem.filevault.stubs

import com.kdiachenko.aem.filevault.integration.dto.VltFilter
import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import java.nio.file.Path

class MetaInfServiceStub : IMetaInfService {
    val createFilterXmlCalls = mutableListOf<Pair<Path, VltFilter>>()

    override fun createFilterXml(tmpDir: Path, vltFilter: VltFilter) {
        createFilterXmlCalls.add(Pair(tmpDir, vltFilter))
    }
}
