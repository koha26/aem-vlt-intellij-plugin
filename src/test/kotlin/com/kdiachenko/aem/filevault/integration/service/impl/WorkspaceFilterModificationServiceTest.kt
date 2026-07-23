package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.application.WriteAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.kdiachenko.aem.filevault.integration.service.FilterModificationStatus
import java.nio.file.Files
import java.nio.file.Path

class WorkspaceFilterModificationServiceTest : BasePlatformTestCase() {
    private lateinit var service: WorkspaceFilterModificationService

    override fun setUp() {
        super.setUp()
        service = WorkspaceFilterModificationService(project)
    }

    fun testInspectReturnsAddedWhenFilterIsMissing() {
        val selection = createSelection(packageName = "missing-filter")

        val preview = service.inspect(selection)

        assertEquals(FilterModificationStatus.ADDED, preview.status)
        assertEquals("/apps/site/components", preview.jcrPath)
        assertNull(preview.filterFingerprint)
    }

    fun testInspectClassifiesExistingCoverageStates() {
        val exact = service.inspect(
            createSelection(
                filterXml = """<workspaceFilter version="1.0"><filter root="/apps/site/components"/></workspaceFilter>""",
                packageName = "exact-root",
            ),
        )
        val covered = service.inspect(
            createSelection(
                filterXml = """<workspaceFilter version="1.0"><filter root="/apps/site"/></workspaceFilter>""",
                packageName = "already-covered",
            ),
        )
        val expansion = service.inspect(
            createSelection(
                filterXml = """<workspaceFilter version="1.0"><filter root="/apps/site/components/button"/></workspaceFilter>""",
                packageName = "expansion-required",
            ),
        )

        assertEquals(FilterModificationStatus.EXACT_ROOT_EXISTS, exact.status)
        assertEquals(FilterModificationStatus.ALREADY_COVERED, covered.status)
        assertEquals(FilterModificationStatus.EXPANSION_CONFIRMATION_REQUIRED, expansion.status)
    }

    fun testInspectReportsInvalidFilter() {
        val selection = createSelection("<workspaceFilter>", "invalid-filter")

        val preview = service.inspect(selection)

        assertEquals(FilterModificationStatus.FILTER_INVALID, preview.status)
        assertNotNull(preview.filterFingerprint)
    }

    fun testApplyCreatesMissingFilterFile() {
        val selection = createSelection(packageName = "create-filter")

        val result = service.apply(service.inspect(selection))
        val filterText = Files.readString(filterPath("create-filter"))

        assertEquals(FilterModificationStatus.ADDED, result.status)
        assertTrue(Files.exists(filterPath("create-filter")))
        assertTrue(filterText.contains("""<filter root="/apps/site/components"/>"""))
    }

    fun testApplyRejectsChangedFingerprint() {
        val selection = createSelection(
            """<workspaceFilter version="1.0"><filter root="/content/site"/></workspaceFilter>""",
            "changed-filter",
        )
        val preview = service.inspect(selection)

        WriteAction.run<RuntimeException> {
            VfsUtil.saveText(
                refresh(filterPath("changed-filter")),
                """<workspaceFilter version="1.0"><filter root="/apps/changed"/></workspaceFilter>""",
            )
        }

        val result = service.apply(preview)

        assertEquals(FilterModificationStatus.FILTER_CHANGED, result.status)
    }

    private fun createSelection(filterXml: String? = null, packageName: String): VirtualFile {
        val selection = packageRoot(packageName).resolve("jcr_root/apps/site/components")
        Files.createDirectories(selection)
        if (filterXml != null) {
            Files.createDirectories(filterPath(packageName).parent)
            Files.writeString(filterPath(packageName), filterXml)
            refresh(filterPath(packageName))
        }
        return refresh(selection)
    }

    private fun packageRoot(packageName: String): Path = Path.of(requireNotNull(project.basePath)).resolve(packageName)

    private fun filterPath(packageName: String): Path = packageRoot(packageName).resolve("META-INF/vault/filter.xml")

    private fun refresh(path: Path): VirtualFile =
        requireNotNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path)) {
            "Virtual file was not created for $path"
        }
}
