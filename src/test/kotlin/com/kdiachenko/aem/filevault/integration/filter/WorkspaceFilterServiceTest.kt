package com.kdiachenko.aem.filevault.integration.filter

import com.kdiachenko.aem.filevault.integration.service.impl.WorkspaceFilterService
import org.apache.jackrabbit.vault.fs.api.ImportMode
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

class WorkspaceFilterServiceTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `fully includes descendant of unconditional root`() {
        val selected = packageSelection("""
            <workspaceFilter version="1.0">
                <filter root="/apps/site"/>
            </workspaceFilter>
        """.trimIndent())

        val result = WorkspaceFilterService().evaluate(selected)

        assertEquals(WorkspaceFilterStatus.FULLY_INCLUDED, result.status)
        assertEquals("/apps/site/components", result.selectedJcrPath)
        assertEquals(listOf("/apps/site"), result.matchingFilterRoots)
        assertNotNull(result.filterFingerprint)
    }

    @ParameterizedTest
    @MethodSource("blockedFilters")
    fun `reports non executable filters`(xml: String?, status: WorkspaceFilterStatus) {
        val result = WorkspaceFilterService().evaluate(packageSelection(xml))
        assertEquals(status, result.status)
    }

    @ParameterizedTest
    @MethodSource("classificationFilters")
    fun `classifies include exclude and overlapping filters`(xml: String, status: WorkspaceFilterStatus) {
        assertEquals(status, WorkspaceFilterService().evaluate(packageSelection(xml)).status)
    }

    @Test
    fun `classifies ancestor and ordered exclusion`() {
        val ancestor = packageSelection("""
            <workspaceFilter version="1.0">
                <filter root="/apps/site/components"/>
                <filter root="/apps/site/clientlibs"/>
            </workspaceFilter>
        """.trimIndent()).parent
        val excluded = packageSelection("""
            <workspaceFilter version="1.0">
                <filter root="/apps/site">
                    <exclude pattern="/apps/site/components(/.*)?"/>
                </filter>
            </workspaceFilter>
        """.trimIndent())

        assertEquals(WorkspaceFilterStatus.PARTIALLY_INCLUDED, WorkspaceFilterService().evaluate(ancestor).status)
        assertEquals(WorkspaceFilterStatus.EXCLUDED, WorkspaceFilterService().evaluate(excluded).status)
    }

    @Test
    fun `classifies regular file content xml and non package path`() {
        val directory = packageSelection("<workspaceFilter><filter root=\"/apps/site\"/></workspaceFilter>")
        val regular = directory.resolve("button.xml").also { Files.writeString(it, "test") }
        assertEquals(WorkspaceFilterStatus.FULLY_INCLUDED, WorkspaceFilterService().evaluate(regular).status)

        val excludedDirectory = packageSelection("<workspaceFilter><filter root=\"/apps/site\"><exclude pattern=\"/apps/site/components(/.*)?\"/></filter></workspaceFilter>")
        val contentXml = excludedDirectory.resolve(".content.xml").also { Files.writeString(it, "<jcr:root/>") }
        assertEquals(WorkspaceFilterStatus.EXCLUDED, WorkspaceFilterService().evaluate(contentXml).status)

        assertEquals(
            WorkspaceFilterStatus.NOT_IN_CONTENT_PACKAGE,
            WorkspaceFilterService().evaluate(tempDir.resolve("outside")).status,
        )
    }

    @Test
    fun `execution scope narrows ancestor roots and retains descendant roots in order`() {
        val selected = packageSelection("""
            <workspaceFilter version="1.0">
                <filter root="/apps" mode="merge_properties">
                    <exclude pattern="/apps/site/private(/.*)?"/>
                </filter>
                <filter root="/apps/site/components" type="cleanup"/>
            </workspaceFilter>
        """.trimIndent())
        val evaluation = WorkspaceFilterService().evaluate(selected)

        val scope = WorkspaceFilterService().executionScope(
            selected,
            WorkspaceFilterOperationOptions(
                partialScopeApproved = true,
                expectedFilterFingerprint = evaluation.filterFingerprint,
            ),
        )
        val reloaded = reload(scope.workspaceFilter)

        assertEquals(listOf("/apps/site/components", "/apps/site/components"), reloaded.filterSets.map { it.root })
        assertEquals(ImportMode.MERGE_PROPERTIES, reloaded.filterSets[0].importMode)
        assertEquals("cleanup", reloaded.filterSets[1].type)
        assertFalse(reloaded.contains("/apps/site/private"))
    }

    @Test
    fun `content xml scope includes node and excludes every descendant`() {
        val directory = packageSelection("<workspaceFilter><filter root=\"/apps/site\"/></workspaceFilter>")
        val contentXml = directory.resolve(".content.xml")
        Files.writeString(contentXml, "<jcr:root/>")
        val evaluation = WorkspaceFilterService().evaluate(contentXml)

        val scope = WorkspaceFilterService().executionScope(
            contentXml,
            WorkspaceFilterOperationOptions(expectedFilterFingerprint = evaluation.filterFingerprint),
        )
        val reloaded = reload(scope.workspaceFilter)

        assertTrue(reloaded.contains("/apps/site/components"))
        assertFalse(reloaded.contains("/apps/site/components/button"))
    }

    @Test
    fun `execution rejects unapproved partial and changed fingerprint`() {
        val selected = packageSelection("<workspaceFilter><filter root=\"/apps/site\"><exclude pattern=\"/apps/site/private(/.*)?\"/></filter></workspaceFilter>")

        assertThrows<WorkspaceFilterBlockedException> {
            WorkspaceFilterService().executionScope(selected, WorkspaceFilterOperationOptions())
        }

        val evaluation = WorkspaceFilterService().evaluate(selected)
        Files.writeString(evaluation.filterFile!!, "<workspaceFilter><filter root=\"/content\"/></workspaceFilter>")
        val changed = assertThrows<WorkspaceFilterBlockedException> {
            WorkspaceFilterService().executionScope(
                selected,
                WorkspaceFilterOperationOptions(
                    partialScopeApproved = true,
                    expectedFilterFingerprint = evaluation.filterFingerprint,
                ),
            )
        }

        assertEquals(WorkspaceFilterStatus.FILTER_CHANGED, changed.validation.status)
    }

    @Test
    fun `effective filter preserves property-only rules`() {
        val selected = packageSelection("""
            <workspaceFilter version="1.0">
                <filter root="/apps/site">
                    <exclude pattern="/apps/site/components/secret" matchProperties="true"/>
                </filter>
            </workspaceFilter>
        """.trimIndent())
        val evaluation = WorkspaceFilterService().evaluate(selected)

        val scope = WorkspaceFilterService().executionScope(
            selected,
            WorkspaceFilterOperationOptions(
                partialScopeApproved = true,
                expectedFilterFingerprint = evaluation.filterFingerprint,
            ),
        )
        val reloaded = reload(scope.workspaceFilter)

        assertTrue(reloaded.contains("/apps/site/components"))
        assertFalse(reloaded.includesProperty("/apps/site/components/secret"))
        assertTrue(reloaded.includesProperty("/apps/site/components/title"))
    }

    private fun packageSelection(filterXml: String?): Path {
        val packageRoot = tempDir.resolve("ui.apps")
        val selected = packageRoot.resolve("jcr_root/apps/site/components")
        Files.createDirectories(selected)
        if (filterXml != null) {
            val filter = packageRoot.resolve("META-INF/vault/filter.xml")
            Files.createDirectories(filter.parent)
            Files.writeString(filter, filterXml)
        }
        return selected
    }

    private fun reload(filter: DefaultWorkspaceFilter): DefaultWorkspaceFilter =
        DefaultWorkspaceFilter().apply {
            load(ByteArrayInputStream(filter.sourceAsString.toByteArray()))
        }

    companion object {
        @JvmStatic
        fun blockedFilters() = Stream.of(
            Arguments.of(null, WorkspaceFilterStatus.FILTER_NOT_FOUND),
            Arguments.of("<workspaceFilter version=\"1.0\"/>", WorkspaceFilterStatus.FILTER_EMPTY),
            Arguments.of("<workspaceFilter>", WorkspaceFilterStatus.FILTER_INVALID),
            Arguments.of(
                "<workspaceFilter><filter root=\"/content/site\"/></workspaceFilter>",
                WorkspaceFilterStatus.OUTSIDE_FILTER,
            ),
            Arguments.of(
                "<workspaceFilter><filter root=\"/apps/site\"><include pattern=\"[\"/></filter></workspaceFilter>",
                WorkspaceFilterStatus.FILTER_INVALID,
            ),
            Arguments.of(
                "<workspaceFilter><filter root=\"/apps/site\" mode=\"unknown\"/></workspaceFilter>",
                WorkspaceFilterStatus.FILTER_INVALID,
            ),
        )

        @JvmStatic
        fun classificationFilters() = Stream.of(
            Arguments.of(
                "<workspaceFilter><filter root=\"/apps/site\"><include pattern=\"/apps/site/clientlibs(/.*)?\"/></filter></workspaceFilter>",
                WorkspaceFilterStatus.EXCLUDED,
            ),
            Arguments.of(
                "<workspaceFilter><filter root=\"/apps/site\"><exclude pattern=\"/apps/site/components(/.*)?\"/><include pattern=\"/apps/site/components/public(/.*)?\"/></filter></workspaceFilter>",
                WorkspaceFilterStatus.PARTIALLY_INCLUDED,
            ),
            Arguments.of(
                "<workspaceFilter><filter root=\"/apps\"/><filter root=\"/apps/site\" type=\"cleanup\"/></workspaceFilter>",
                WorkspaceFilterStatus.FULLY_INCLUDED,
            ),
        )
    }
}
