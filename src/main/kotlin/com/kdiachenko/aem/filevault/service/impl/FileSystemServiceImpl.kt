package com.kdiachenko.aem.filevault.service.impl

import com.intellij.openapi.diagnostic.Logger
import com.kdiachenko.aem.filevault.service.FileChangeTracker
import com.kdiachenko.aem.filevault.service.FileSystemService
import com.kdiachenko.aem.filevault.service.dto.OperationAction
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*
import kotlin.io.path.createTempDirectory

class FileSystemServiceImpl : FileSystemService {
    private val logger = Logger.getInstance(FileSystemServiceImpl::class.java)

    override fun createTempDirectory(): Path {
        return createTempDirectory("aem-filevault-pull-${UUID.randomUUID()}")
    }

    override fun copyDirectory(source: Path, target: Path, tracker: FileChangeTracker) {
        if (!Files.exists(source)) {
            logger.warn("Source directory does not exist: $source")
            return
        }

        // Check if target exists and create if needed
        if (!Files.exists(target)) {
            Files.createDirectories(target)
            tracker.addChange(OperationAction.ADDED, target.toString(), "Created directory")
        }

        // First, identify files that exist in target but not in source (to be deleted)
        if (Files.exists(target) && Files.isDirectory(target)) {
            Files.walkFileTree(target, object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val relativePath = target.relativize(file)
                    val sourceFile = source.resolve(relativePath)

                    if (!Files.exists(sourceFile)) {
                        Files.delete(file)
                        tracker.addChange(OperationAction.DELETED, file.toString(), "File not in source")
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    if (dir != target) {  // Don't delete the target directory itself
                        val relativePath = target.relativize(dir)
                        val sourceDir = source.resolve(relativePath)

                        if (!Files.exists(sourceDir)) {
                            Files.delete(dir)
                            tracker.addChange(OperationAction.DELETED, dir.toString(), "Directory not in source")
                        }
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        }

        // Then, copy files from source to target
        Files.walkFileTree(source, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val targetDir = target.resolve(source.relativize(dir))
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir)
                    tracker.addChange(OperationAction.ADDED, targetDir.toString(), "Created directory")
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val targetFile = target.resolve(source.relativize(file))

                if (!Files.exists(targetFile)) {
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                    tracker.addChange(OperationAction.ADDED, targetFile.toString(), "New file")
                } else {
                    // Check if content is different
                    if (!Files.isSameFile(file, targetFile) &&
                        !Files.readAllBytes(file).contentEquals(Files.readAllBytes(targetFile))
                    ) {
                        Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                        tracker.addChange(OperationAction.UPDATED, targetFile.toString(), "Content changed")
                    } else {
                        tracker.addChange(OperationAction.NOT_TOUCHED, targetFile.toString(), "Content unchanged")
                    }
                }
                return FileVisitResult.CONTINUE
            }
        })
    }

    override fun deleteDirectory(directory: Path?) {
        if (directory == null || !Files.exists(directory)) {
            return
        }

        Files.walkFileTree(directory, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                Files.delete(file)
                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                Files.delete(dir)
                return FileVisitResult.CONTINUE
            }
        })

        logger.info("Deleted temporary directory: $directory")
    }
}
