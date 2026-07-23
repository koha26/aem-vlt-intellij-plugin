package com.kdiachenko.aem.filevault.integration.service

import com.intellij.openapi.vfs.VirtualFile
import com.kdiachenko.aem.filevault.integration.filter.ContentPackageContext

enum class FilterModificationStatus {
    ADDED,
    ALREADY_COVERED,
    EXACT_ROOT_EXISTS,
    EXPANSION_CONFIRMATION_REQUIRED,
    FILTER_INVALID,
    FILTER_CHANGED,
    NOT_APPLICABLE,
}

data class FilterModificationPreview(
    val status: FilterModificationStatus,
    val selection: VirtualFile,
    val context: ContentPackageContext?,
    val jcrPath: String?,
    val filterFingerprint: String?,
    val message: String,
)

data class FilterModificationResult(
    val status: FilterModificationStatus,
    val filterFile: VirtualFile?,
    val message: String,
)

interface IWorkspaceFilterModificationService {
    fun inspect(selection: VirtualFile): FilterModificationPreview
    fun apply(preview: FilterModificationPreview): FilterModificationResult
}
