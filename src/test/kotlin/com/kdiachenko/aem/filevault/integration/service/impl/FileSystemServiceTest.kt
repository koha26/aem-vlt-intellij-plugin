package com.kdiachenko.aem.filevault.integration.service.impl

import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import com.kdiachenko.aem.filevault.integration.service.FileChangeTracker
import com.kdiachenko.aem.filevault.integration.service.IFileSystemService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class FileSystemServiceTest {

    @TempDir
    @JvmField
    var tempFolder: Path? = null

    lateinit var service: IFileSystemService

    @BeforeEach
    fun setUp() {
        service = FileSystemService.getInstance()
    }

    @Test
    fun testCreateTempDirectory() {
        val tempDir = service.createTempDirectory()

        try {
            assertTrue(Files.exists(tempDir))
            assertTrue(Files.isDirectory(tempDir))
            assertTrue(tempDir.toString().contains("aem-filevault-"))
        } finally {
            service.deleteDirectory(tempDir)
        }
    }

    @Test
    fun testDeleteDirectoryRemovesAllContents() {
        val tempDir = tempFolder?.resolve("testdir") ?: throw Exception("Temp dir is null")
        Files.createDirectory(tempDir)

        val subDir = tempDir.resolve("subdir")
        Files.createDirectory(subDir)
        val file1 = tempDir.resolve("file1.txt")
        Files.write(file1, "test content".toByteArray())
        val file2 = subDir.resolve("file2.txt")
        Files.write(file2, "test content in subdir".toByteArray())

        service.deleteDirectory(tempDir)

        assertFalse(Files.exists(tempDir))
        assertFalse(Files.exists(subDir))
        assertFalse(Files.exists(file1))
        assertFalse(Files.exists(file2))
    }

    @Test
    fun testCopyFileCreatesNewFile() {
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        sourceDir.toFile().mkdir()
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")
        targetDir.toFile().mkdir()

        try {
            val sourceFile = sourceDir.resolve("test.txt")
            Files.write(sourceFile, "test content".toByteArray())
            val targetFile = targetDir.resolve("test.txt")
            val tracker = FileChangeTracker()

            service.copyFile(sourceFile, targetFile, tracker)

            assertTrue(Files.exists(targetFile))
            assertEquals("test content", String(Files.readAllBytes(targetFile)))
            assertEquals(1, tracker.changes.size)
            assertEquals(OperationAction.ADDED, tracker.changes[0].action)
            assertEquals(targetFile.toString(), tracker.changes[0].path)
        } finally {
            service.deleteDirectory(sourceDir)
            service.deleteDirectory(targetDir)
        }
    }

    @Test
    fun testCopyFileUpdatesExistingFile() {
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        sourceDir.toFile().mkdir()
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")
        targetDir.toFile().mkdir()

        try {
            val sourceFile = sourceDir.resolve("test.txt")
            Files.write(sourceFile, "initial content".toByteArray())
            val targetFile = targetDir.resolve("test.txt")
            Files.write(targetFile, "another content".toByteArray())
            val tracker = FileChangeTracker()

            service.copyFile(sourceFile, targetFile, tracker)
            Files.write(sourceFile, "modified content".toByteArray())
            service.copyFile(sourceFile, targetFile, tracker)

            assertEquals("modified content", String(Files.readAllBytes(targetFile)))
            assertEquals(2, tracker.changes.size)
            assertEquals(OperationAction.UPDATED, tracker.changes[1].action)
        } finally {
            service.deleteDirectory(sourceDir)
            service.deleteDirectory(targetDir)
        }
    }

    @Test
    fun testCopyFileDontUpdateExistingFile() {
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        sourceDir.toFile().mkdir()
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")
        targetDir.toFile().mkdir()

        try {
            val sourceFile = sourceDir.resolve("test.txt")
            Files.write(sourceFile, "initial content".toByteArray())
            val tracker = FileChangeTracker()

            service.copyFile(sourceFile, sourceFile, tracker)

            assertEquals("initial content", String(Files.readAllBytes(sourceFile)))
            assertEquals(1, tracker.changes.size)
            assertEquals(OperationAction.NOTHING_CHANGED, tracker.changes[0].action)
        } finally {
            service.deleteDirectory(sourceDir)
            service.deleteDirectory(targetDir)
        }
    }

    @Test
    fun testCopyDirectoryCopiesStructure() {
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        sourceDir.toFile().mkdir()
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")
        targetDir.toFile().mkdir()

        try {
            val sourceSubDir = sourceDir.resolve("subdir")
            Files.createDirectory(sourceSubDir)
            val sourceFile1 = sourceDir.resolve("file1.txt")
            Files.write(sourceFile1, "test content 1".toByteArray())
            val sourceFile2 = sourceSubDir.resolve("file2.txt")
            Files.write(sourceFile2, "test content 2".toByteArray())

            val tracker = FileChangeTracker()
            service.copyDirectory(sourceDir, targetDir, tracker)

            val targetSubDir = targetDir.resolve("subdir")
            val targetFile1 = targetDir.resolve("file1.txt")
            val targetFile2 = targetSubDir.resolve("file2.txt")

            assertTrue(Files.exists(targetSubDir))
            assertTrue(Files.exists(targetFile1))
            assertTrue(Files.exists(targetFile2))
            assertEquals("test content 1", String(Files.readAllBytes(targetFile1)))
            assertEquals("test content 2", String(Files.readAllBytes(targetFile2)))
            assertTrue(tracker.changes.size >= 3)
        } finally {
            service.deleteDirectory(sourceDir)
            service.deleteDirectory(targetDir)
        }
    }

    @Test
    fun testCopyDirectoryUpdatesModifiedFiles() {
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        sourceDir.toFile().mkdir()
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")
        targetDir.toFile().mkdir()

        try {
            val sourceFile = sourceDir.resolve("file.txt")
            Files.write(sourceFile, "initial content".toByteArray())

            service.copyDirectory(sourceDir, targetDir, FileChangeTracker())
            Files.write(sourceFile, "modified content".toByteArray())

            val tracker = FileChangeTracker()
            service.copyDirectory(sourceDir, targetDir, tracker)

            assertEquals("modified content", String(Files.readAllBytes(targetDir.resolve("file.txt"))))
            assertTrue(tracker.changes.isNotEmpty())
        } finally {
            service.deleteDirectory(sourceDir)
            service.deleteDirectory(targetDir)
        }
    }

    @Test
    fun testCopyDirectoryDeletesRemovedFiles() {
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        sourceDir.toFile().mkdir()
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")
        targetDir.toFile().mkdir()

        try {
            val sourceFile = sourceDir.resolve("file.txt")
            Files.write(sourceFile, "test content".toByteArray())

            service.copyDirectory(sourceDir, targetDir, FileChangeTracker())
            Files.delete(sourceFile)

            val tracker = FileChangeTracker()
            service.copyDirectory(sourceDir, targetDir, tracker)

            assertFalse(Files.exists(targetDir.resolve("file.txt")))
            assertTrue(tracker.changes.isNotEmpty())
        } finally {
            service.deleteDirectory(sourceDir)
            service.deleteDirectory(targetDir)
        }
    }
}
