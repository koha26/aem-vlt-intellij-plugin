package com.kdiachenko.aem.filevault.integration.dto

/**
 * Represents the type of operation for a file
 */
enum class OperationAction {
    ADDED,
    UPDATED,
    DELETED,
    ERROR,
    NOTHING_CHANGED;

    companion object {
        fun fromString(action: String): OperationAction {
            return when (action) {
                "A" -> ADDED
                "U" -> UPDATED
                "D" -> DELETED
                "E" -> ERROR
                else -> NOTHING_CHANGED
            }
        }
    }
}
