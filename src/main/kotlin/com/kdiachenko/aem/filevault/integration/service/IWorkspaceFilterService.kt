package com.kdiachenko.aem.filevault.integration.service

import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterExecutionScope
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterOperationOptions
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterValidationResult
import java.nio.file.Path

interface IWorkspaceFilterService {
    fun evaluate(selection: Path): WorkspaceFilterValidationResult

    fun executionScope(
        selection: Path,
        options: WorkspaceFilterOperationOptions,
    ): WorkspaceFilterExecutionScope
}
