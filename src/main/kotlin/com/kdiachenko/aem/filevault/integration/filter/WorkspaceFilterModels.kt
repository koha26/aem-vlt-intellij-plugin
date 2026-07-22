package com.kdiachenko.aem.filevault.integration.filter

import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter
import java.nio.file.Path

enum class WorkspaceFilterStatus {
    FULLY_INCLUDED,
    PARTIALLY_INCLUDED,
    EXCLUDED,
    OUTSIDE_FILTER,
    FILTER_NOT_FOUND,
    FILTER_EMPTY,
    FILTER_INVALID,
    FILTER_CHANGED,
    NOT_IN_CONTENT_PACKAGE,
}

data class WorkspaceFilterValidationResult(
    val status: WorkspaceFilterStatus,
    val selectedLocalPath: Path,
    val selectedJcrPath: String?,
    val packageRoot: Path?,
    val filterFile: Path?,
    val matchingFilterRoots: List<String> = emptyList(),
    val effectiveFilterRoots: List<String> = emptyList(),
    val filterFingerprint: String? = null,
    val message: String,
    val addToFilterApplicable: Boolean,
)

data class WorkspaceFilterOperationOptions(
    val partialScopeApproved: Boolean = false,
    val expectedFilterFingerprint: String? = null,
)

data class WorkspaceFilterExecutionScope(
    val validation: WorkspaceFilterValidationResult,
    val workspaceFilter: DefaultWorkspaceFilter,
)

class WorkspaceFilterBlockedException(
    val validation: WorkspaceFilterValidationResult,
) : IllegalStateException(validation.message)
