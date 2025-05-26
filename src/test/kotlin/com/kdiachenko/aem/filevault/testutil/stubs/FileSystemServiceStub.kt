package com.kdiachenko.aem.filevault.stubs

import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import com.kdiachenko.aem.filevault.integration.service.FileChangeTracker
import com.kdiachenko.aem.filevault.integration.service.IFileSystemService
import java.nio.file.Path

class FileSystemServiceStub(val tempDir: Path) : IFileSystemService {
    var copiedDirectories = mutableListOf<Pair<Path, Path>>()
    var copiedFiles = mutableListOf<Pair<Path, Path>>()
    var deletedDirectories = mutableListOf<Path?>()

    override fun createTempDirectory(): Path {
        return tempDir
    }

    override fun copyDirectory(source: Path, target: Path, tracker: FileChangeTracker) {
        copiedDirectories.add(Pair(source, target))
        tracker.addChange(OperationAction.UPDATED, source.toString())
    }

    override fun deleteDirectory(directory: Path?) {
        deletedDirectories.add(directory)
    }

    override fun copyFile(source: Path, target: Path, tracker: FileChangeTracker) {
        copiedFiles.add(Pair(source, target))
        tracker.addChange(OperationAction.UPDATED, source.toString())
    }
}
