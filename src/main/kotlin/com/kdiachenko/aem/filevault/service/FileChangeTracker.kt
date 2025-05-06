package com.kdiachenko.aem.filevault.service

import com.kdiachenko.aem.filevault.service.dto.OperationAction

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
 * Entry for an operation tracked by ProgressTrackerListener
 */
data class OperationEntry(
    val action: String,
    val path: String,
    val message: String? = null
)

/**
 * Entry for a file change
 */
data class FileChangeEntry(
    val action: OperationAction,
    val path: String,
    val reason: String
)
