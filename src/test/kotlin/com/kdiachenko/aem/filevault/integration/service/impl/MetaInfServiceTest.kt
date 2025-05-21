package com.kdiachenko.aem.filevault.integration.service.impl

import com.kdiachenko.aem.filevault.integration.dto.VltFilter
import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class MetaInfServiceTest {

    @TempDir
    @JvmField
    var tempFolder: Path? = null

    private lateinit var tempDir: Path
    private lateinit var service: IMetaInfService

    @BeforeEach
    fun setUp() {
        service = MetaInfService.getInstance()
    }

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

    private fun createMetaInfTestDir(): Path {
        val resolve = tempFolder?.resolve("meta-inf-test") ?: throw Exception("Temp dir is null")
        resolve.toFile().mkdir()
        return resolve
    }


    private fun createAndVerifyFilterXml(filter: VltFilter, expectedContent: String) {
        val tempDir = tempFolder ?: throw Exception("Temp dir is null")
        try {
            service.createFilterXml(tempDir, filter)

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
