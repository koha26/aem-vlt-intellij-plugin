package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.openapi.components.service
import com.kdiachenko.aem.filevault.integration.dto.VltFilter
import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import java.nio.file.Files
import java.nio.file.Path

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

    override fun createFilterXml(tmpDir: Path, vltFilter: VltFilter) {
        val metaInfDir = tmpDir.resolve("META-INF/vault")
        Files.createDirectories(metaInfDir)
        val filterFile = metaInfDir.resolve("filter.xml")

        val filterContent = generateFilterXmlContent(vltFilter)
        Files.write(filterFile, filterContent.toByteArray())
    }

    private fun generateFilterXmlContent(vltFilter: VltFilter): String {
        val filterAttributes = buildString {
            append("root=\"${vltFilter.root}\"")
            if (vltFilter.mode.isNotEmpty()) {
                append(" mode=\"${vltFilter.mode}\"")
            }
        }

        val filterTag = buildFilterTag(vltFilter, filterAttributes)

        return """
        <?xml version="1.0" encoding="UTF-8"?>
        <workspaceFilter version="1.0">
            $filterTag
        </workspaceFilter>
        """.trimIndent()
    }

    private fun buildFilterTag(vltFilter: VltFilter, filterAttributes: String): String {
        val hasChildTags = vltFilter.excludePatterns.isNotEmpty() || vltFilter.includePatterns.isNotEmpty()

        return if (!hasChildTags) {
            "<filter $filterAttributes/>"
        } else {
            buildString {
                append("<filter $filterAttributes>")
                vltFilter.includePatterns.forEach { pattern ->
                    append("<include pattern=\"$pattern\"/>")
                }
                vltFilter.excludePatterns.forEach { pattern ->
                    append("<exclude pattern=\"$pattern\"/>")
                }
                append("</filter>")
            }
        }
    }
}
