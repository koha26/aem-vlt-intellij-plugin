package com.kdiachenko.aem.filevault.integration.service

import com.kdiachenko.aem.filevault.integration.dto.OperationAction

/**
 * Tracks file changes during directory operations
 */
class FileChangeTracker {
    private val _changes = mutableListOf<FileChangeEntry>()
    val changes: List<FileChangeEntry> get() = _changes

    fun addChange(action: OperationAction, path: String, reason: String = "") {
        _changes.add(FileChangeEntry(action, path, reason))
    }
}

/**
 * Entry for a file change
 */
data class FileChangeEntry(
    val action: OperationAction,
    val path: String,
    val reason: String
)
