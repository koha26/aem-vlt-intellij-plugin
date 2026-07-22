package com.kdiachenko.aem.filevault.integration.filter

import com.kdiachenko.aem.filevault.util.JcrPathUtil.normalizeJcrPath
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter
import java.nio.file.Path

enum class ScopedPathDecision {
    INCLUDE,
    TRAVERSE_ONLY,
    EXCLUDE,
}

fun interface ScopedPathPolicy {
    fun decide(relativePath: Path, isDirectory: Boolean): ScopedPathDecision
}

class WorkspaceFilterScopedPathPolicy(
    private val selectedJcrPath: String,
    private val filter: WorkspaceFilter,
) : ScopedPathPolicy {
    override fun decide(relativePath: Path, isDirectory: Boolean): ScopedPathDecision {
        val suffix = relativePath.iterator().asSequence().joinToString("/") { it.toString() }
        val path = (if (suffix.isEmpty()) {
            selectedJcrPath
        } else {
            "$selectedJcrPath/$suffix"
        }).normalizeJcrPath()
        return when {
            filter.contains(path) -> ScopedPathDecision.INCLUDE
            isDirectory && (filter.covers(path) || filter.isAncestor(path)) -> ScopedPathDecision.TRAVERSE_ONLY
            else -> ScopedPathDecision.EXCLUDE
        }
    }
}
