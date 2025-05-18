package com.kdiachenko.aem.filevault.integration.service.impl

import com.kdiachenko.aem.filevault.integration.dto.VltFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path

class MetaInfServiceTest {

    @Rule
    @JvmField
    var tempFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()
    private lateinit var tempDir: Path

    @Test
    fun testCreateFilterXmlWithoutModeAndPatterns() {
        tempDir = createMetaInfTestDir()

        val filter = VltFilter(root = "/content/test")
        createAndVerifyFilterXml(
            filter, """
            <?xml version="1.0" encoding="UTF-8"?>
            <workspaceFilter version="1.0">
                <filter root="/content/test"/>
            </workspaceFilter>
        """.trimIndent()
        )
    }

    @Test
    fun testCreateFilterXmlWithMode() {
        tempDir = createMetaInfTestDir()

        val filter = VltFilter(
            root = "/content/test",
            mode = "merge"
        )

        createAndVerifyFilterXml(
            filter, """
            <?xml version="1.0" encoding="UTF-8"?>
            <workspaceFilter version="1.0">
                <filter root="/content/test" mode="merge"/>
            </workspaceFilter>
        """.trimIndent()
        )
    }

    @Test
    fun testCreateFilterXmlWithIncludeAndExcludePatterns() {
        tempDir = createMetaInfTestDir()

        val filter = VltFilter(
            root = "/content/test",
            mode = "merge",
            includePatterns = listOf("/content/test/include1", "/content/test/include2"),
            excludePatterns = listOf("/content/test/exclude1", "/content/test/exclude2")
        )

        createAndVerifyFilterXml(
            filter, """
            <?xml version="1.0" encoding="UTF-8"?>
            <workspaceFilter version="1.0">
                <filter root="/content/test" mode="merge"><include pattern="/content/test/include1"/><include pattern="/content/test/include2"/><exclude pattern="/content/test/exclude1"/><exclude pattern="/content/test/exclude2"/></filter>
            </workspaceFilter>
        """.trimIndent()
        )
    }

    private fun createMetaInfTestDir(): Path =
        tempFolder.newFolder("meta-inf-test").toPath()

    private fun createAndVerifyFilterXml(filter: VltFilter, expectedContent: String) {
        try {
            MetaInfService.createFilterXml(tempDir, filter)

            val metaInfDir = tempDir.resolve("META-INF/vault")
            assertTrue(Files.exists(metaInfDir))
            assertTrue(Files.isDirectory(metaInfDir))

            val filterFile = metaInfDir.resolve("filter.xml")
            assertTrue(Files.exists(filterFile))

            val content = String(Files.readAllBytes(filterFile))
            assertEquals(expectedContent, content)
        } finally {
            deleteDirectory(tempDir)
        }
    }

    private fun deleteDirectory(directory: Path) {
        if (Files.exists(directory)) {
            Files.walk(directory)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::delete)
        }
    }
}
