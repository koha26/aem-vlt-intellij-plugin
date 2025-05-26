package com.kdiachenko.aem.filevault.integration.service.impl

import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import com.kdiachenko.aem.filevault.integration.service.FileChangeTracker
import com.kdiachenko.aem.filevault.integration.service.IFileSystemService
import com.kdiachenko.aem.filevault.testutil.dsl.structure
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class FileSystemServiceTest {

    @TempDir
    @JvmField
    var tempFolder: Path? = null

    lateinit var service: IFileSystemService

    @BeforeEach
    fun setUp() {
        service = FileSystemService()
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
        tempFolder?.structure {
            folder("testdir") {
                folder("subdir") {
                    file("file2.txt", "test content in subdir")
                }
                file("file1.txt", "test content")
            }
        }
        val tempDir = tempFolder?.resolve("testdir") ?: throw Exception("Temp dir is null")

        service.deleteDirectory(tempDir)

        assertFalse(Files.exists(tempDir))
        assertFalse(Files.exists(tempDir.resolve("subdir")))
        assertFalse(Files.exists(tempDir.resolve("subdir/file2.txt")))
        assertFalse(Files.exists(tempDir.resolve("file1.txt")))
    }

    @Test
    fun testCopyFileCreatesNewFile() {
        tempFolder?.structure {
            folder("source") {
                file("test.txt", "test content")
            }
            folder("target")
        }
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        val sourceFile = sourceDir.resolve("test.txt")
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")
        val targetFile = targetDir.resolve("test.txt")

        try {
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
        tempFolder?.structure {
            folder("source") {
                file("test.txt", "initial content")
            }
            folder("target") {
                file("test.txt", "another content")
            }
        }
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        val sourceFile = sourceDir.resolve("test.txt")
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")
        val targetFile = targetDir.resolve("test.txt")

        try {
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
        tempFolder?.structure {
            folder("source") {
                file("test.txt", "initial content")
            }
        }
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        val sourceFile = sourceDir.resolve("test.txt")
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")

        try {
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
        tempFolder?.structure {
            folder("source") {
                folder("subdir") {
                    file("file2.txt", "test content 2")
                }
                file("file1.txt", "test content 1")
            }
        }
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")

        try {
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
        tempFolder?.structure {
            folder("source") {
                file("file.txt", "initial content")
            }
        }
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        val sourceFile = sourceDir.resolve("file.txt")
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")

        try {
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
        tempFolder?.structure {
            folder("source") {
                file("file.txt", "test content")
            }
        }
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        val sourceFile = sourceDir.resolve("file.txt")
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")

        try {
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

    @Test
    fun testDeleteDirectoryWithNullPath() {
        service.deleteDirectory(null)
    }

    @Test
    fun testDeleteNonExistentDirectory() {
        val nonExistentDir = Paths.get("/non/existent/directory")
        service.deleteDirectory(nonExistentDir)
    }

    @Test
    fun testCopyDirectoryWithNonExistentSource() {
        tempFolder?.structure {
            folder("target")
        }
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")

        try {
            val tracker = FileChangeTracker()
            service.copyDirectory(sourceDir, targetDir, tracker)

            assertEquals(0, tracker.changes.size)
        } finally {
            service.deleteDirectory(targetDir)
        }
    }

    @Test
    fun testCopyDirectoryWithNonExistentTarget() {
        tempFolder?.structure {
            folder("source") {
                file("file.txt", "test content")
            }
        }
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        sourceDir.toFile().mkdir()
        val targetDir = tempFolder?.resolve("non-existent-target") ?: throw Exception("Temp dir is null")

        try {

            val tracker = FileChangeTracker()
            service.copyDirectory(sourceDir, targetDir, tracker)

            assertTrue(Files.exists(targetDir))
            assertTrue(Files.exists(targetDir.resolve("file.txt")))

            assertTrue(tracker.changes.size >= 1)

            assertEquals(OperationAction.ADDED, tracker.changes[0].action)
            assertEquals(targetDir.toString(), tracker.changes[0].path)
        } finally {
            service.deleteDirectory(sourceDir)
            service.deleteDirectory(targetDir)
        }
    }

    @Test
    fun testCopyDirectoryDeletesDirectoryNotInSource() {
        tempFolder?.structure {
            folder("source") {
                file("file.txt", "test content")
            }
            folder("target") {
                folder("subdir-to-delete") {
                    file("file-to-delete.txt", "content to delete")
                }
            }
        }
        val sourceDir = tempFolder?.resolve("source") ?: throw Exception("Temp dir is null")
        val targetDir = tempFolder?.resolve("target") ?: throw Exception("Temp dir is null")

        try {
            val targetSubDir = targetDir.resolve("subdir-to-delete")
            val targetFile = targetSubDir.resolve("file-to-delete.txt")

            val tracker = FileChangeTracker()
            service.copyDirectory(sourceDir, targetDir, tracker)

            assertFalse(Files.exists(targetSubDir))
            assertFalse(Files.exists(targetFile))

            assertTrue(tracker.changes.size >= 2)

            assertTrue(tracker.changes.any { it.action == OperationAction.DELETED })
        } finally {
            service.deleteDirectory(sourceDir)
            service.deleteDirectory(targetDir)
        }
    }
}
