package com.kdiachenko.aem.filevault.integration.service.impl

import com.kdiachenko.aem.filevault.integration.service.Filter
import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import java.nio.file.Files
import java.nio.file.Path

/**
 * Implementation of MetaInfService
 */
class MetaInfService : IMetaInfService {

    override fun createFilterXml(tmpDir: Path, filter: Filter) {
        val metaInfDir = tmpDir.resolve("META-INF/vault")
        Files.createDirectories(metaInfDir)

        val filterFile = metaInfDir.resolve("filter.xml")

        val filterTagBuilder = StringBuilder()
        filterTagBuilder.append("<filter root=\"${filter.root}\"")
        if (filter.mode.isNotEmpty()) {
            filterTagBuilder.append(" mode=\"${filter.mode}\"")
        }
        val hasChildTags = filter.excludePatterns.isNotEmpty() || filter.includePatterns.isNotEmpty()
        if (hasChildTags) {
            filterTagBuilder.append(">")
            filter.excludePatterns.forEach { pattern ->
                filterTagBuilder.append("<exclude pattern=\"$pattern\"/>")
            }
            filter.includePatterns.forEach { pattern ->
                filterTagBuilder.append("<include pattern=\"$pattern\"/>")
            }
            filterTagBuilder.append("</filter>")
        } else {
            filterTagBuilder.append("/>")
        }
        val filterContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <workspaceFilter version="1.0">
                $filterTagBuilder
            </workspaceFilter>
        """.trimIndent()

        Files.write(filterFile, filterContent.toByteArray())
    }
}
