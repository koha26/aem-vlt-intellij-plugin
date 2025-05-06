package com.kdiachenko.aem.filevault.integration.service

import java.nio.file.Path

/**
 * Interface for file system operations
 */
interface IFileSystemService {
    fun createTempDirectory(): Path
    fun copyDirectory(source: Path, target: Path, tracker: FileChangeTracker)
    fun deleteDirectory(directory: Path?)
}
