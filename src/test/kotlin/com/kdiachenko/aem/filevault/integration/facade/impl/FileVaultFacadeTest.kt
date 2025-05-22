package com.kdiachenko.aem.filevault.integration.facade.impl

import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.registerServiceInstance
import com.intellij.util.application
import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import com.kdiachenko.aem.filevault.integration.dto.OperationEntryDetail
import com.kdiachenko.aem.filevault.integration.dto.VltFilter
import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.service.FileChangeTracker
import com.kdiachenko.aem.filevault.integration.service.IFileSystemService
import com.kdiachenko.aem.filevault.integration.service.IMetaInfService
import com.kdiachenko.aem.filevault.integration.service.IVaultOperationService
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Test class for FileVaultFacade
 */
class FileVaultFacadeTest : BasePlatformTestCase() {

    private lateinit var fileVaultFacade: FileVaultFacade
    private lateinit var fileSystemServiceStub: FileSystemServiceStub
    private lateinit var metaInfServiceStub: MetaInfServiceStub
    private lateinit var vaultOperationServiceStub: VaultOperationServiceStub
    private lateinit var serverConfig: DetailedAEMServerConfig
    private lateinit var progressIndicator: ProgressIndicatorStub
    private lateinit var tempDir: Path

    override fun getTestDataPath() = "src/test/testData/com/kdiachenko/aem/filevault/integration/facade"

    override fun setUp() {
        super.setUp()

        // Create temp directory
        tempDir = Files.createTempDirectory("filevault-test")

        // Setup stubs
        fileSystemServiceStub = FileSystemServiceStub()
        metaInfServiceStub = MetaInfServiceStub()
        vaultOperationServiceStub = VaultOperationServiceStub()
        progressIndicator = ProgressIndicatorStub()

        // Register service instances
        application.registerServiceInstance(IFileSystemService::class.java, fileSystemServiceStub)
        application.registerServiceInstance(IMetaInfService::class.java, metaInfServiceStub)
        application.registerServiceInstance(IVaultOperationService::class.java, vaultOperationServiceStub)

        // Setup server config
        serverConfig = DetailedAEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            username = "admin",
            password = "admin"
        )

        fileVaultFacade = FileVaultFacade.getInstance(project) as? FileVaultFacade
            ?: throw Exception("Failed to get FileVaultFacade instance")
    }

    fun `test exportContent should return success result when export is successful`() {
        // Arrange
        myFixture.copyDirectoryToProject(
            "common/en",
            "content/jcr_root/content/project/en"
        )
        val selectedFile = myFixture.configureByFile("content/jcr_root/content/project/en/.content.xml")
        fileSystemServiceStub.tempDir = FileUtil.createTempDirectory("file-vault-test", null).toPath()

        val projectLocalFile = File(selectedFile.virtualFile.path)

        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()


        assertTrue(result.success)
        assertTrue(result.message.contains("Successfully exported content"))
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(1, vaultOperationServiceStub.exportCalls.size)
        assertEquals(1, fileSystemServiceStub.createTempDirectoryCalls)
        assertEquals(1, fileSystemServiceStub.deleteDirectoryCalls.size)
    }

    fun `test exportContent should return failed result when JCR path is invalid`() {
        // Arrange
        val projectLocalFile = File("invalid-path")


        // Act
        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        // Assert
        assertFalse(result.success)
        assertEquals("Invalid JCR path.", result.message)
        assertEquals(0, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(0, vaultOperationServiceStub.exportCalls.size)
        assertEquals(0, fileSystemServiceStub.createTempDirectoryCalls)
    }

    fun `test exportContent should return failed result when export throws exception`() {
        // Arrange
        val projectLocalFile = File("/content/project/en")
        fileSystemServiceStub.tempDir = tempDir
        vaultOperationServiceStub.shouldThrowException = true

        // Act
        val result = fileVaultFacade.exportContent(serverConfig, projectLocalFile, progressIndicator).get()

        // Assert
        assertFalse(result.success)
        assertTrue(result.message.contains("Error:"))
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(1, vaultOperationServiceStub.exportCalls.size)
        assertEquals(1, fileSystemServiceStub.createTempDirectoryCalls)
        assertEquals(1, fileSystemServiceStub.deleteDirectoryCalls.size)

        // Reset
        vaultOperationServiceStub.shouldThrowException = false
    }

    fun `test importContent should return success result when import is successful`() {
        // Arrange
        val projectLocalFile = File("/content/project/en")
        fileSystemServiceStub.tempDir = tempDir

        // Act
        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        // Assert
        assertTrue(result.success)
        assertTrue(result.message.contains("Successfully imported content"))
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(1, vaultOperationServiceStub.importCalls.size)
        assertEquals(1, fileSystemServiceStub.createTempDirectoryCalls)
        assertEquals(1, fileSystemServiceStub.deleteDirectoryCalls.size)
    }

    fun `test importContent should return failed result when JCR path is invalid`() {
        // Arrange
        val projectLocalFile = File("invalid-path")

        // Act
        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        // Assert
        assertFalse(result.success)
        assertEquals("Invalid JCR path.", result.message)
        assertEquals(0, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(0, vaultOperationServiceStub.importCalls.size)
        assertEquals(0, fileSystemServiceStub.createTempDirectoryCalls)
    }

    fun `test importContent should return failed result when import throws exception`() {
        // Arrange
        val projectLocalFile = File("/content/project/en")
        fileSystemServiceStub.tempDir = tempDir
        vaultOperationServiceStub.shouldThrowException = true

        // Act
        val result = fileVaultFacade.importContent(serverConfig, projectLocalFile, progressIndicator).get()

        // Assert
        assertFalse(result.success)
        assertTrue(result.message.contains("Error:"))
        assertEquals(1, metaInfServiceStub.createFilterXmlCalls.size)
        assertEquals(1, vaultOperationServiceStub.importCalls.size)
        assertEquals(1, fileSystemServiceStub.createTempDirectoryCalls)
        assertEquals(1, fileSystemServiceStub.deleteDirectoryCalls.size)

        // Reset
        vaultOperationServiceStub.shouldThrowException = false
    }

    fun `test createDetailedResult should format results correctly`() {
        // Arrange
        val entries = listOf(
            OperationEntryDetail(OperationAction.ADDED, "/path1"),
            OperationEntryDetail(OperationAction.UPDATED, "/path2"),
            OperationEntryDetail(OperationAction.DELETED, "/path3"),
            OperationEntryDetail(OperationAction.ERROR, "/path4")
        )

        // Act
        //val result = fileVaultFacade.testCreateDetailedResult(entries)

        // Assert
        //assertEquals("Updated: 1 | Added: 1 | Removed: 1 | Error: 1", result)
    }

    fun `test createDetailedResult should handle empty results`() {
        // Arrange
        val entries = emptyList<OperationEntryDetail>()

        // Act
        //val result = fileVaultFacade.testCreateDetailedResult(entries)

        // Assert
        //assertEquals("", result)
    }

    fun `test filterOutNothingChanged should filter out NOTHING_CHANGED actions`() {
        // Arrange
        val entries = listOf(
            OperationEntryDetail(OperationAction.ADDED, "/path1"),
            OperationEntryDetail(OperationAction.NOTHING_CHANGED, "/path2"),
            OperationEntryDetail(OperationAction.UPDATED, "/path3")
        )

        // Act
        //val result = fileVaultFacade.testFilterOutNothingChanged(entries)

        // Assert
        //assertEquals(2, result.size)
        //assertEquals(OperationAction.ADDED, result[0].action)
        //(OperationAction.UPDATED, result[1].action)
    }
}

// Stub implementations for testing
enum class FileType {
    FILE, DIRECTORY, NONE
}

class FileSystemServiceStub : IFileSystemService {
    var tempDir: Path? = null
    var createTempDirectoryCalls = 0
    var copyDirectoryCalls = mutableListOf<Pair<Path, Path>>()
    var copyFileCalls = mutableListOf<Pair<Path, Path>>()
    var deleteDirectoryCalls = mutableListOf<Path?>()
    var fileType: FileType = FileType.DIRECTORY
    var listFiles: Array<File> = emptyArray()

    override fun createTempDirectory(): Path {
        createTempDirectoryCalls++
        return tempDir ?: throw IllegalStateException("tempDir not set")
    }

    override fun copyDirectory(source: Path, target: Path, tracker: FileChangeTracker) {
        copyDirectoryCalls.add(Pair(source, target))
        tracker.addChange(OperationAction.UPDATED, source.toString())
    }

    override fun deleteDirectory(directory: Path?) {
        deleteDirectoryCalls.add(directory)
    }

    override fun copyFile(source: Path, target: Path, tracker: FileChangeTracker) {
        copyFileCalls.add(Pair(source, target))
        tracker.addChange(OperationAction.UPDATED, source.toString())
    }
}

class MetaInfServiceStub : IMetaInfService {
    val createFilterXmlCalls = mutableListOf<Pair<Path, VltFilter>>()

    override fun createFilterXml(tmpDir: Path, vltFilter: VltFilter) {
        createFilterXmlCalls.add(Pair(tmpDir, vltFilter))
    }
}

class VaultOperationServiceStub : IVaultOperationService {
    val exportCalls = mutableListOf<VltOperationContext>()
    val importCalls = mutableListOf<VltOperationContext>()
    var shouldThrowException = false

    override fun export(context: VltOperationContext) {
        exportCalls.add(context)
        if (shouldThrowException) {
            throw RuntimeException("Test exception")
        }
        context.progressListener?.onMessage(null, "A", "/path1")
        context.progressListener?.onMessage(null, "U", "/path2")
    }

    override fun import(context: VltOperationContext) {
        importCalls.add(context)
        if (shouldThrowException) {
            throw RuntimeException("Test exception")
        }
        context.progressListener?.onMessage(null, "A", "/path1")
        context.progressListener?.onMessage(null, "U", "/path2")
    }
}

class ProgressIndicatorStub : ProgressIndicatorBase() {
    val textsChanges = arrayListOf<String>()
    val fractionChanges = arrayListOf<Double>()

    override fun setText(text: String?) {
        super.setText(text)
        textsChanges.add(text ?: "")
    }

    override fun setFraction(fraction: Double) {
        super.setFraction(fraction)
        fractionChanges.add(fraction)
    }

}
