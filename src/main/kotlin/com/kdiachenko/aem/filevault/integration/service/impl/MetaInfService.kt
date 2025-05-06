package com.kdiachenko.aem.filevault.integration.service.impl

import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import java.nio.file.Files
import java.nio.file.Path

/**
 * Implementation of MetaInfService
 */
class MetaInfService : IMetaInfService {

    override fun createFilterXml(tmpDir: Path, jcrPath: String) {
        val metaInfDir = tmpDir.resolve("META-INF/vault")
        Files.createDirectories(metaInfDir)

        val filterFile = metaInfDir.resolve("filter.xml")

        val filterContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <workspaceFilter version="1.0">
                <filter root="$jcrPath"/>
            </workspaceFilter>
        """.trimIndent()

        Files.write(filterFile, filterContent.toByteArray())
    }
}
