package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.openapi.components.service
import com.kdiachenko.aem.filevault.integration.filter.ContentPackageContext
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterBlockedException
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterExecutionScope
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterOperationOptions
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterStatus
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterValidationResult
import com.kdiachenko.aem.filevault.integration.service.IWorkspaceFilterService
import com.kdiachenko.aem.filevault.util.JcrPathUtil.resolveContentPackage
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toNormalizedJcrPath
import org.apache.jackrabbit.vault.fs.api.PathFilterSet
import org.apache.jackrabbit.vault.fs.config.DefaultWorkspaceFilter
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

class WorkspaceFilterService : IWorkspaceFilterService {

    companion object {
        @JvmStatic
        fun getInstance(): IWorkspaceFilterService = service()
    }

    override fun evaluate(selection: Path): WorkspaceFilterValidationResult {
        val absolute = selection.toAbsolutePath().normalize()
        val context = absolute.resolveContentPackage()
            ?: return blocked(
                status = WorkspaceFilterStatus.NOT_IN_CONTENT_PACKAGE,
                selected = absolute,
                jcrPath = null,
                context = null,
                message = "Selection is not under jcr_root.",
                addApplicable = false,
            )
        val jcrPath = absolute.toNormalizedJcrPath(context)
        if (!Files.exists(context.filterFile)) {
            return blocked(
                status = WorkspaceFilterStatus.FILTER_NOT_FOUND,
                selected = absolute,
                jcrPath = jcrPath,
                context = context,
                message = "This content package has no META-INF/vault/filter.xml.",
                addApplicable = true,
            )
        }

        val bytes = Files.readAllBytes(context.filterFile)
        val fingerprint = sha256(bytes)
        val filter = try {
            DefaultWorkspaceFilter().apply {
                load(ByteArrayInputStream(bytes))
            }
        } catch (e: Exception) {
            return blocked(
                status = WorkspaceFilterStatus.FILTER_INVALID,
                selected = absolute,
                jcrPath = jcrPath,
                context = context,
                message = "filter.xml is invalid: ${e.message ?: e.javaClass.simpleName}",
                addApplicable = false,
                fingerprint = fingerprint,
            )
        }

        val related = filter.filterSets.filter { set ->
            set.covers(jcrPath) || set.isAncestor(jcrPath)
        }
        if (filter.filterSets.isEmpty()) {
            return blocked(
                status = WorkspaceFilterStatus.FILTER_EMPTY,
                selected = absolute,
                jcrPath = jcrPath,
                context = context,
                message = "The workspace filter has no filter roots.",
                addApplicable = true,
                fingerprint = fingerprint,
            )
        }
        if (related.isEmpty()) {
            return blocked(
                status = WorkspaceFilterStatus.OUTSIDE_FILTER,
                selected = absolute,
                jcrPath = jcrPath,
                context = context,
                message = "$jcrPath is outside the workspace filter.",
                addApplicable = true,
                fingerprint = fingerprint,
            )
        }

        val isDirectory = Files.isDirectory(absolute)
        val status = when {
            !isDirectory && filter.contains(jcrPath) -> WorkspaceFilterStatus.FULLY_INCLUDED
            !isDirectory -> WorkspaceFilterStatus.EXCLUDED
            related.any { it.root != jcrPath && it.isAncestor(jcrPath) } -> WorkspaceFilterStatus.PARTIALLY_INCLUDED
            filter.contains(jcrPath) && related.none { it.entries.isNotEmpty() } -> WorkspaceFilterStatus.FULLY_INCLUDED
            filter.contains(jcrPath) -> WorkspaceFilterStatus.PARTIALLY_INCLUDED
            related.any { set ->
                set.root.startsWith("$jcrPath/") || set.entries.any { entry ->
                    entry.isInclude && includeTargetsSelectedSubtree(entry.filter, jcrPath)
                }
            } ->
                WorkspaceFilterStatus.PARTIALLY_INCLUDED
            else -> WorkspaceFilterStatus.EXCLUDED
        }

        return WorkspaceFilterValidationResult(
            status = status,
            selectedLocalPath = absolute,
            selectedJcrPath = jcrPath,
            packageRoot = context.packageRoot,
            filterFile = context.filterFile,
            matchingFilterRoots = related.map { it.root },
            effectiveFilterRoots = effectiveRoots(related, jcrPath, isDirectory),
            filterFingerprint = fingerprint,
            message = message(status, jcrPath, related.map { it.root }),
            addToFilterApplicable = status != WorkspaceFilterStatus.FULLY_INCLUDED,
        )
    }

    override fun executionScope(
        selection: Path,
        options: WorkspaceFilterOperationOptions,
    ): WorkspaceFilterExecutionScope {
        val validation = evaluate(selection)
        if (validation.status != WorkspaceFilterStatus.FULLY_INCLUDED &&
            validation.status != WorkspaceFilterStatus.PARTIALLY_INCLUDED
        ) {
            throw WorkspaceFilterBlockedException(validation)
        }
        throw UnsupportedOperationException("Task 3 implements execution scope.")
    }

    private fun blocked(
        status: WorkspaceFilterStatus,
        selected: Path,
        jcrPath: String?,
        context: ContentPackageContext?,
        message: String,
        addApplicable: Boolean,
        fingerprint: String? = null,
    ) = WorkspaceFilterValidationResult(
        status = status,
        selectedLocalPath = selected,
        selectedJcrPath = jcrPath,
        packageRoot = context?.packageRoot,
        filterFile = context?.filterFile,
        filterFingerprint = fingerprint,
        message = message,
        addToFilterApplicable = addApplicable,
    )

    private fun effectiveRoots(
        sets: List<PathFilterSet>,
        selected: String,
        directory: Boolean,
    ): List<String> = sets.mapNotNull { set ->
        when {
            set.covers(selected) -> selected
            directory && set.root.startsWith("$selected/") -> set.root
            else -> null
        }
    }

    private fun message(
        status: WorkspaceFilterStatus,
        selected: String,
        roots: List<String>,
    ): String = when (status) {
        WorkspaceFilterStatus.FULLY_INCLUDED -> "$selected is included by the FileVault filter."
        WorkspaceFilterStatus.PARTIALLY_INCLUDED -> "Only part of $selected is included. Effective roots: ${roots.joinToString()}."
        WorkspaceFilterStatus.EXCLUDED -> "$selected is excluded by the FileVault filter."
        WorkspaceFilterStatus.OUTSIDE_FILTER -> "$selected is outside the FileVault filter."
        WorkspaceFilterStatus.FILTER_NOT_FOUND -> "This content package has no META-INF/vault/filter.xml."
        WorkspaceFilterStatus.FILTER_EMPTY -> "The workspace filter has no filter roots."
        WorkspaceFilterStatus.FILTER_INVALID -> "filter.xml is invalid."
        WorkspaceFilterStatus.FILTER_CHANGED -> "filter.xml changed after validation."
        WorkspaceFilterStatus.NOT_IN_CONTENT_PACKAGE -> "Selection is not under jcr_root."
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }

    private fun includeTargetsSelectedSubtree(filter: Any, selected: String): Boolean {
        val pattern = (filter as? DefaultPathFilter)?.pattern ?: return false
        return pattern == selected ||
            pattern.startsWith("$selected/") ||
            pattern.startsWith("\\Q$selected/") ||
            pattern.startsWith("\\Q$selected\\E/")
    }
}
