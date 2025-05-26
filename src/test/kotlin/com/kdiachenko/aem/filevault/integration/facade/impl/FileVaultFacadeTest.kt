package com.kdiachenko.aem.filevault.integration.facade.impl

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerServiceInstance
import com.intellij.testFramework.unregisterService
import com.intellij.util.application
import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import com.kdiachenko.aem.filevault.integration.dto.OperationEntryDetail
import com.kdiachenko.aem.filevault.integration.dto.VltFilter
import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.facade.IFileVaultFacade
import com.kdiachenko.aem.filevault.integration.service.IFileSystemService
import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import com.kdiachenko.aem.filevault.integration.service.IVaultOperationService
import com.kdiachenko.aem.filevault.integration.service.impl.MetaInfService
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import com.kdiachenko.aem.filevault.stubs.FileSystemServiceStub
import com.kdiachenko.aem.filevault.stubs.MetaInfServiceStub
import com.kdiachenko.aem.filevault.stubs.ProgressIndicatorStub
import com.kdiachenko.aem.filevault.stubs.VaultOperationServiceStub
import com.kdiachenko.aem.filevault.testutil.dsl.structure
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString

/**
 * Test class for [FileVaultFacade]
 */
class FileVaultFacadeTest : BasePlatformTestCase() {

    private lateinit var fileVaultFacade: FileVaultFacade
    private lateinit var fileSystemServiceStub: FileSystemServiceStub
    private lateinit var metaInfServiceStub: MetaInfServiceStub
    private lateinit var vaultOperationServiceStub: VaultOperationServiceStub
    private lateinit var serverConfig: DetailedAEMServerConfig
    private lateinit var progressIndicator: ProgressIndicatorStub
    private lateinit var tempDir: Path

    override fun setUp() {
        super.setUp()

        tempDir = createTempDirectory(getTestName(true))
        progressIndicator = ProgressIndicatorStub()

        fileSystemServiceStub = registerFileSystemService()
        metaInfServiceStub = registerMetaInfServiceStub()
        vaultOperationServiceStub = registerVaultOperationServiceStub()

        serverConfig = DetailedAEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            username = "admin",
            password = "admin"
        )

        fileVaultFacade = createNewFileFaultFacade()
    }

    override fun tearDown() {
        FileUtil.deleteRecursively(tempDir)
        super.tearDown()
    }

    fun `test exportContent should return success result when export is successful`() {
        val sourceDirectory = createSourceDirectory()
        createSimpleProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").toFile()

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue("Export should succeed but failed with: ${result.message}", result.success)
        assertTrue(
            "Message should contain 'Successfully exported content' but was: ${result.message}",
            result.message.contains("Successfully exported content")
        )
    }

    fun `test exportContent should return showing progress indicator during the export process`() {
        val sourceDirectory = createSourceDirectory()
        createSimpleProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").toFile()

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue("Export should succeed but failed with: ${result.message}", result.success)
        assertEquals(5, progressIndicator.textsChanges.size)
        assertEquals(
            listOf(
                "Preparing export operation...", "Exporting content from AEM...", "Processing exported content...",
                "Cleaning up...", ""
            ), progressIndicator.textsChanges
        )
        assertEquals(5, progressIndicator.fractionChanges.size)
        assertEquals(listOf(0.1, 0.2, 0.7, 0.9, 1.0), progressIndicator.fractionChanges)
    }

    fun `test exportContent should create filter_xml file when selected file is file`() {
        val sourceDirectory = createSourceDirectory()
        createSimpleProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").toFile()

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue("Export should succeed but failed with: ${result.message}", result.success)
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(tempDir.pathString, metaInfServiceStub.createFilterXmlCalls[0].first.pathString)
        assertEquals(VltFilter("/content/project/en"), metaInfServiceStub.createFilterXmlCalls[0].second)
    }

    fun `test exportContent should create filter_xml file when selected file is folder`() {
        val sourceDirectory = createSourceDirectory()
        createSimpleProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en").toFile()

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue("Export should succeed but failed with: ${result.message}", result.success)
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(tempDir.pathString, metaInfServiceStub.createFilterXmlCalls[0].first.pathString)
        assertEquals(VltFilter("/content/project/en"), metaInfServiceStub.createFilterXmlCalls[0].second)
    }

    fun `test exportContent should create filter_xml file when selected file has closest folders`() {
        val sourceDirectory = createSourceDirectory()
        createComplexProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").toFile()

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue("Export should succeed but failed with: ${result.message}", result.success)
        val metaInfServiceStub = MetaInfService.getInstance() as MetaInfServiceStub
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(tempDir.pathString, metaInfServiceStub.createFilterXmlCalls[0].first.pathString)
        assertEquals(VltFilter("/content/project/en"), metaInfServiceStub.createFilterXmlCalls[0].second)
    }

    fun `test exportContent should copy selected file`() {
        val sourceDirectory = createSourceDirectory()
        createComplexProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").toFile()

        vaultOperationServiceStub = registerVaultOperationServiceStub(object : VaultOperationServiceStub() {
            override fun export(context: VltOperationContext) {
                tempDir.structure {
                    folder("jcr_root/content/project/en") {
                        file(".content.xml")
                        folder("nested")
                        folder("clientlibs")
                    }
                }
            }
        })
        fileVaultFacade = createNewFileFaultFacade()

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue(result.success)
        assertEquals(0, fileSystemServiceStub.copiedDirectories.size)
        assertEquals(1, fileSystemServiceStub.copiedFiles.size)
        val copiedFile = fileSystemServiceStub.copiedFiles[0]
        assertEquals(
            tempDir.resolve("jcr_root/content/project/en/.content.xml").pathString,
            copiedFile.first.pathString
        )
        assertEquals(
            sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").pathString,
            copiedFile.second.pathString
        )
    }

    fun `test exportContent should copy selected directory`() {
        val sourceDirectory = createSourceDirectory()
        createComplexProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en").toFile()

        vaultOperationServiceStub = registerVaultOperationServiceStub(object : VaultOperationServiceStub() {
            override fun export(context: VltOperationContext) {
                tempDir.structure {
                    folder("jcr_root/content/project/en") {
                        file(".content.xml")
                        folder("nested")
                        folder("clientlibs")
                    }
                }
            }
        })
        fileVaultFacade = createNewFileFaultFacade()

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue(result.success)
        assertEquals(1, fileSystemServiceStub.copiedDirectories.size)
        val copiedDirectory = fileSystemServiceStub.copiedDirectories[0]
        assertEquals(
            tempDir.resolve("jcr_root/content/project/en").pathString,
            copiedDirectory.first.pathString
        )
        assertEquals(
            sourceDirectory.resolve("jcr_root/content/project/en").pathString,
            copiedDirectory.second.pathString
        )
        assertEquals(0, fileSystemServiceStub.copiedFiles.size)
    }

    fun `test exportContent should return failed result when JCR path is invalid`() {
        val sourceDirectory = createSourceDirectory()
        sourceDirectory.structure {
            file("invalid-path")
        }
        val projectLocalFile = sourceDirectory.resolve("invalid-path").toFile()

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertFalse(result.success)
        assertEquals("Invalid JCR path.", result.message)
    }

    fun `test exportContent should return failed result when export throws exception`() {
        val sourceDirectory = createSourceDirectory()
        createSimpleProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").toFile()

        val vaultOperationServiceStub =
            application.getService(IVaultOperationService::class.java) as VaultOperationServiceStub
        vaultOperationServiceStub.shouldThrowException = true

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertFalse("Export should fail but succeeded", result.success)
        assertTrue(
            "Message should contain 'Error:' but was: ${result.message}",
            result.message.contains("Error:")
        )
    }

    fun `test importContent should return success result when import is successful`() {
        val sourceDirectory = createSourceDirectory()
        createSimpleProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en").toFile()

        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue("Import should succeed but failed with: ${result.message}", result.success)
        assertTrue(
            "Message should contain 'Successfully imported content' but was: ${result.message}",
            result.message.contains("Successfully imported content")
        )
    }

    fun `test importContent should return showing progress indicator during the import process`() {
        val sourceDirectory = createSourceDirectory()
        createComplexProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en").toFile()

        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue(result.success)
        assertEquals(5, progressIndicator.textsChanges.size)
        assertEquals(
            listOf(
                "Preparing import operation...", "Preparing content for import...", "Importing content to AEM...",
                "Cleaning up...", ""
            ), progressIndicator.textsChanges
        )
        assertEquals(5, progressIndicator.fractionChanges.size)
        assertEquals(listOf(0.1, 0.3, 0.5, 0.9, 1.0), progressIndicator.fractionChanges)
    }

    fun `test importContent should create filter_xml file when selected file is file`() {
        val sourceDirectory = createSourceDirectory()
        createSimpleProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").toFile()

        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue("Export should succeed but failed with: ${result.message}", result.success)
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(tempDir.pathString, metaInfServiceStub.createFilterXmlCalls[0].first.pathString)
        assertEquals(VltFilter("/content/project/en"), metaInfServiceStub.createFilterXmlCalls[0].second)
    }

    fun `test importContent should create filter_xml file when selected file is folder`() {
        val sourceDirectory = createSourceDirectory()
        createComplexProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en").toFile()

        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue("Export should succeed but failed with: ${result.message}", result.success)
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(tempDir.pathString, metaInfServiceStub.createFilterXmlCalls[0].first.pathString)
        assertEquals(VltFilter("/content/project/en"), metaInfServiceStub.createFilterXmlCalls[0].second)
    }

    fun `test importContent should create filter_xml file when selected file has closest folders`() {
        val sourceDirectory = createSourceDirectory()
        createComplexProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").toFile()

        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue("Export should succeed but failed with: ${result.message}", result.success)
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(tempDir.pathString, metaInfServiceStub.createFilterXmlCalls[0].first.pathString)
        val expected = VltFilter(
            "/content/project/en",
            "",
            listOf("/content/project/en/clientlibs(/.*)?", "/content/project/en/nested(/.*)?")
        )
        val actualVltFilter = metaInfServiceStub.createFilterXmlCalls[0].second
        assertEquals(expected.root, actualVltFilter.root)
        assertEquals(expected.mode, actualVltFilter.mode)
        assertSameElements(expected.excludePatterns, actualVltFilter.excludePatterns)
        assertEquals(expected.includePatterns, actualVltFilter.includePatterns)
    }

    fun `test importContent should copy selected file before import`() {
        val sourceDirectory = createSourceDirectory()
        createComplexProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").toFile()

        vaultOperationServiceStub = registerVaultOperationServiceStub(object : VaultOperationServiceStub() {
            override fun import(context: VltOperationContext) {
                context.progressListener?.onMessage(null, "-", "/content")
                context.progressListener?.onMessage(null, "-", "/content/project")
                context.progressListener?.onMessage(null, "-", "/content/project/en")
                context.progressListener?.onMessage(null, "A", "/content/project/en/.content.xml")
            }
        })
        fileVaultFacade = createNewFileFaultFacade()

        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue(result.success)
        assertEquals(
            """
Successfully imported content from /content/project/en/.content.xml.
<br/>Nodes changes statistic:
 | Added: 1
        """.trimIndent(), result.message.trimIndent()
        )
        assertEquals(1, result.entries.size)
        assertEquals(OperationEntryDetail(OperationAction.ADDED, "/content/project/en/.content.xml"), result.entries[0])
        assertEquals(0, fileSystemServiceStub.copiedDirectories.size)
        assertEquals(1, fileSystemServiceStub.copiedFiles.size)
        val copiedFile = fileSystemServiceStub.copiedFiles[0]
        assertEquals(
            sourceDirectory.resolve("jcr_root/content/project/en/.content.xml").pathString,
            copiedFile.first.pathString
        )
        assertEquals(
            tempDir.resolve("jcr_root/content/project/en/.content.xml").pathString,
            copiedFile.second.pathString
        )
    }

    fun `test importContent should copy selected folder before import`() {
        val sourceDirectory = createSourceDirectory()
        createComplexProjectSourceStructure(sourceDirectory)
        val projectLocalFile = sourceDirectory.resolve("jcr_root/content/project/en").toFile()

        vaultOperationServiceStub = registerVaultOperationServiceStub(object : VaultOperationServiceStub() {
            override fun import(context: VltOperationContext) {
                context.progressListener?.onMessage(null, "-", "/content")
                context.progressListener?.onMessage(null, "-", "/content/project")
                context.progressListener?.onMessage(null, "-", "/content/project/en")
                context.progressListener?.onMessage(null, "U", "/content/project/en/.content.xml")
                context.progressListener?.onMessage(null, "-", "/content/project/en/nested")
                context.progressListener?.onMessage(null, "D", "/content/project/en/nested/nested.txt")
                context.progressListener?.onError(null, "/content/project/en/clientlibs", Exception("Error"))
            }
        })
        fileVaultFacade = createNewFileFaultFacade()

        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertTrue(result.success)
        assertEquals(
            """
Successfully imported content from /content/project/en.
<br/>Nodes changes statistic:
Updated: 1 | Removed: 1 | Error: 1
        """.trimIndent(), result.message.trimIndent()
        )
        assertEquals(3, result.entries.size)
        assertEquals(
            OperationEntryDetail(OperationAction.UPDATED, "/content/project/en/.content.xml"),
            result.entries[0]
        )
        assertEquals(
            OperationEntryDetail(OperationAction.DELETED, "/content/project/en/nested/nested.txt"),
            result.entries[1]
        )
        assertEquals(
            OperationEntryDetail(OperationAction.ERROR, "/content/project/en/clientlibs", "Error"),
            result.entries[2]
        )

        assertEquals(1, fileSystemServiceStub.copiedDirectories.size)
        val copiedDirectory = fileSystemServiceStub.copiedDirectories[0]
        assertEquals(
            sourceDirectory.resolve("jcr_root/content/project/en").pathString,
            copiedDirectory.first.pathString
        )
        assertEquals(
            tempDir.resolve("jcr_root/content/project/en").pathString,
            copiedDirectory.second.pathString
        )
        assertEquals(0, fileSystemServiceStub.copiedFiles.size)
    }

    fun `test importContent should return failed result when JCR path is invalid`() {
        val sourceDirectory = createSourceDirectory()
        sourceDirectory.structure {
            file("invalid-path")
        }
        val projectLocalFile = sourceDirectory.resolve("invalid-path").toFile()

        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertFalse(result.success)
        assertEquals("Invalid JCR path.", result.message)
    }

    fun `test importContent should return failed result when import throws exception`() {
        val sourceDirectory = createSourceDirectory()
        createSimpleProjectSourceStructure(sourceDirectory)
        val projectLocalFile = tempDir.resolve("jcr_root/content/project/en").toFile()

        vaultOperationServiceStub.shouldThrowException = true

        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        assertFalse("Import should fail but succeeded", result.success)
        assertTrue(
            "Message should contain 'Error:' but was: ${result.message}",
            result.message.contains("Error:")
        )
    }

    fun `test filterOutNothingChanged should filter out NOTHING_CHANGED actions`() {
        val entries = listOf(
            OperationEntryDetail(OperationAction.ADDED, "/path1"),
            OperationEntryDetail(OperationAction.NOTHING_CHANGED, "/path2"),
            OperationEntryDetail(OperationAction.UPDATED, "/path3")
        )

        val result = fileVaultFacade.run { entries.filterOutNothingChanged() }

        assertEquals(2, result.size)
        assertEquals(OperationAction.ADDED, result[0].action)
        assertEquals(OperationAction.UPDATED, result[1].action)
    }

    fun `test createDetailedResult should format results correctly with all action types`() {
        val entries = listOf(
            OperationEntryDetail(OperationAction.ADDED, "/path1"),
            OperationEntryDetail(OperationAction.UPDATED, "/path2"),
            OperationEntryDetail(OperationAction.DELETED, "/path3"),
            OperationEntryDetail(OperationAction.ERROR, "/path4")
        )

        val result = fileVaultFacade.createDetailedResult(entries)

        assertEquals("Updated: 1 | Added: 1 | Removed: 1 | Error: 1", result)
    }

    fun `test createDetailedResult should format results correctly with some action types`() {
        val entries = listOf(
            OperationEntryDetail(OperationAction.ADDED, "/path1"),
            OperationEntryDetail(OperationAction.UPDATED, "/path2")
        )

        val result = fileVaultFacade.createDetailedResult(entries)

        assertEquals("Updated: 1 | Added: 1", result)
    }

    fun `test createDetailedResult should handle empty results`() {
        val entries = emptyList<OperationEntryDetail>()

        val result = fileVaultFacade.createDetailedResult(entries)

        assertEquals("", result)
    }

    fun `test getInstance should return service instance`() {
        val instance = FileVaultFacade.getInstance(project)

        assertNotNull(instance)
        assertTrue(instance is FileVaultFacade)
    }

    fun createNewFileFaultFacade(): FileVaultFacade {
        project.unregisterService(IFileVaultFacade::class.java)
        project.registerServiceInstance(IFileVaultFacade::class.java, FileVaultFacade())
        return FileVaultFacade.getInstance(project) as FileVaultFacade
    }

    private fun createSourceDirectory(): Path = createTempDirectory(getTestName(true) + "-source")

    private fun createSimpleProjectSourceStructure(sourceDirectory: Path) {
        sourceDirectory.structure {
            folder("jcr_root/content/project/en") {
                file(".content.xml")
            }
        }
    }

    private fun createComplexProjectSourceStructure(sourceDirectory: Path) {
        sourceDirectory.structure {
            folder("jcr_root/content/project/en") {
                file(".content.xml")
                folder("nested")
                folder("clientlibs")
            }
        }
    }

    private fun registerFileSystemService(): FileSystemServiceStub =
        registerFileSystemService(FileSystemServiceStub(tempDir))

    private fun registerFileSystemService(stub: FileSystemServiceStub): FileSystemServiceStub {
        application.unregisterService(IFileSystemService::class.java)
        application.registerServiceInstance(IFileSystemService::class.java, stub)
        return application.getService(IFileSystemService::class.java) as FileSystemServiceStub
    }

    private fun registerMetaInfServiceStub(): MetaInfServiceStub = registerMetaInfServiceStub(MetaInfServiceStub())

    private fun registerMetaInfServiceStub(stub: MetaInfServiceStub): MetaInfServiceStub {
        application.unregisterService(IMetaInfService::class.java)
        application.registerServiceInstance(IMetaInfService::class.java, stub)
        return application.getService(IMetaInfService::class.java) as MetaInfServiceStub
    }

    private fun registerVaultOperationServiceStub(): VaultOperationServiceStub =
        registerVaultOperationServiceStub(VaultOperationServiceStub())

    private fun registerVaultOperationServiceStub(stub: VaultOperationServiceStub): VaultOperationServiceStub {
        application.unregisterService(IVaultOperationService::class.java)
        application.registerServiceInstance(IVaultOperationService::class.java, stub)
        return application.getService(IVaultOperationService::class.java) as VaultOperationServiceStub
    }
}
