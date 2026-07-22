package com.kdiachenko.aem.filevault.stubs

import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterBlockedException
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterExecutionScope
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterOperationOptions
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterStatus
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterValidationResult
import com.kdiachenko.aem.filevault.integration.service.IWorkspaceFilterService
import com.kdiachenko.aem.filevault.util.JcrPathUtil.resolveContentPackage
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toJcrPath
import org.apache.jackrabbit.vault.fs.api.PathFilterSet
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter
import java.nio.file.Path

class WorkspaceFilterServiceStub : IWorkspaceFilterService {
    val evaluateCalls = mutableListOf<Path>()
    val executionCalls = mutableListOf<Pair<Path, WorkspaceFilterOperationOptions>>()
    lateinit var validation: WorkspaceFilterValidationResult
    var scope: WorkspaceFilterExecutionScope? = null

    fun allow(value: WorkspaceFilterExecutionScope) {
        scope = value
        validation = value.validation
    }

    fun blockWith(status: WorkspaceFilterStatus, selection: Path) {
        scope = null
        validation = WorkspaceFilterValidationResult(
            status = status,
            selectedLocalPath = selection,
            selectedJcrPath = selection.toString().toJcrPath(),
            packageRoot = selection.resolveContentPackage()?.packageRoot,
            filterFile = selection.resolveContentPackage()?.filterFile,
            message = status.name,
            addToFilterApplicable = true,
        )
    }

    override fun evaluate(selection: Path): WorkspaceFilterValidationResult {
        evaluateCalls.add(selection)
        return if (::validation.isInitialized) {
            validation
        } else {
            defaultScopeFor(selection).validation
        }
    }

    override fun executionScope(selection: Path, options: WorkspaceFilterOperationOptions): WorkspaceFilterExecutionScope {
        executionCalls.add(selection to options)
        if (scope == null && ::validation.isInitialized && validation.status != WorkspaceFilterStatus.FULLY_INCLUDED) {
            throw WorkspaceFilterBlockedException(validation)
        }
        val value = scope ?: defaultScopeFor(selection)
        validation = value.validation
        return value
    }

    private fun defaultScopeFor(selection: Path): WorkspaceFilterExecutionScope {
        val jcrPath = selection.toString().toJcrPath() ?: "/"
        val filter = DefaultWorkspaceFilter().apply {
            add(PathFilterSet(jcrPath))
        }
        val context = selection.resolveContentPackage()
        val selectedPath = selection.toAbsolutePath().normalize()
        val validation = WorkspaceFilterValidationResult(
            status = WorkspaceFilterStatus.FULLY_INCLUDED,
            selectedLocalPath = selectedPath,
            selectedJcrPath = jcrPath,
            packageRoot = context?.packageRoot,
            filterFile = context?.filterFile,
            matchingFilterRoots = listOf(jcrPath),
            effectiveFilterRoots = listOf(jcrPath),
            filterFingerprint = "fingerprint",
            message = "Included",
            addToFilterApplicable = false,
        )
        return WorkspaceFilterExecutionScope(validation, filter)
    }
}
