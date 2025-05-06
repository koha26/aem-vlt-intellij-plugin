package com.kdiachenko.aem.filevault.service.impl

import com.intellij.openapi.diagnostic.Logger
import com.kdiachenko.aem.filevault.service.MetaInfService
import java.nio.file.Files
import java.nio.file.Path

/**
 * Implementation of MetaInfService
 */
class MetaInfServiceImpl : MetaInfService {
    private val logger = Logger.getInstance(MetaInfServiceImpl::class.java)

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
        logger.info("Created filter.xml at $filterFile with content:\n$filterContent")
    }
}
