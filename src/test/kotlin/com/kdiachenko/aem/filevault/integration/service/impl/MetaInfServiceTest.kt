package com.kdiachenko.aem.filevault.integration.service.impl

import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import org.apache.jackrabbit.vault.fs.api.ImportMode
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
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
        service = MetaInfService()
    }

    @Test
    fun `createFilterXml copies workspace filter roots and rules`() {
        tempDir = createMetaInfTestDir()
        val workspaceFilter = DefaultWorkspaceFilter().apply {
            load(
                ByteArrayInputStream(
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <workspaceFilter version="1.0">
                        <filter root="/content/test" mode="merge_properties">
                            <include pattern="/content/test(/.*)?"/>
                            <exclude pattern="/content/test/private(/.*)?"/>
                        </filter>
                        <filter root="/conf/test" type="cleanup"/>
                    </workspaceFilter>
                    """.trimIndent().toByteArray()
                )
            )
        }

        service.createFilterXml(tempDir, workspaceFilter)

        val metaInfDir = tempDir.resolve("META-INF/vault")
        assertTrue(Files.exists(metaInfDir))
        assertTrue(Files.isDirectory(metaInfDir))

        val filterFile = metaInfDir.resolve("filter.xml")
        assertTrue(Files.exists(filterFile))

        val reloaded = DefaultWorkspaceFilter().apply {
            load(filterFile.toFile())
        }
        assertEquals(listOf("/content/test", "/conf/test"), reloaded.filterSets.map { it.root })
        assertEquals(ImportMode.MERGE_PROPERTIES, reloaded.filterSets[0].importMode)
        assertEquals("cleanup", reloaded.filterSets[1].type)
        assertTrue(reloaded.contains("/content/test/en"))
        assertFalse(reloaded.contains("/content/test/private"))
    }

    private fun createMetaInfTestDir(): Path {
        val resolve = tempFolder?.resolve("meta-inf-test") ?: throw Exception("Temp dir is null")
        resolve.toFile().mkdir()
        return resolve
    }
}
