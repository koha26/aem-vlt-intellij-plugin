package com.kdiachenko.aem.filevault.stubs

import com.kdiachenko.aem.filevault.integration.filter.ScopedPathPolicy
import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import com.kdiachenko.aem.filevault.integration.service.FileChangeTracker
import com.kdiachenko.aem.filevault.integration.service.IFileSystemService
import java.nio.file.Path

class FileSystemServiceStub(val tempDir: Path) : IFileSystemService {
    data class SynchronizedDirectoryCall(
        val source: Path,
        val target: Path,
        val policy: ScopedPathPolicy,
    )

    var copiedDirectories = mutableListOf<Pair<Path, Path>>()
    var copiedFiles = mutableListOf<Pair<Path, Path>>()
    var deletedDirectories = mutableListOf<Path?>()
    var synchronizedDirectories = mutableListOf<SynchronizedDirectoryCall>()

    override fun createTempDirectory(): Path {
        return tempDir
    }

    override fun copyDirectory(source: Path, target: Path, tracker: FileChangeTracker) {
        copiedDirectories.add(Pair(source, target))
        tracker.addChange(OperationAction.UPDATED, source.toString())
    }

    override fun synchronizeDirectory(source: Path, target: Path, tracker: FileChangeTracker, policy: ScopedPathPolicy) {
        synchronizedDirectories.add(SynchronizedDirectoryCall(source, target, policy))
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
