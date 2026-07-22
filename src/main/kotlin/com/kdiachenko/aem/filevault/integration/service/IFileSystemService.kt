package com.kdiachenko.aem.filevault.integration.service

import com.kdiachenko.aem.filevault.integration.filter.ScopedPathPolicy
import java.nio.file.Path

/**
 * Interface for file system operations
 */
interface IFileSystemService {
    fun createTempDirectory(): Path
    fun copyDirectory(source: Path, target: Path, tracker: FileChangeTracker)
    fun synchronizeDirectory(source: Path, target: Path, tracker: FileChangeTracker, policy: ScopedPathPolicy)
    fun deleteDirectory(directory: Path?)
    fun copyFile(source: Path, target: Path, tracker: FileChangeTracker)
}
