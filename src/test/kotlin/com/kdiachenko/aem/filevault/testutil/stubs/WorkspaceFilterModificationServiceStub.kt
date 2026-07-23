package com.kdiachenko.aem.filevault.stubs

import com.intellij.openapi.vfs.VirtualFile
import com.kdiachenko.aem.filevault.integration.service.FilterModificationPreview
import com.kdiachenko.aem.filevault.integration.service.FilterModificationResult
import com.kdiachenko.aem.filevault.integration.service.IWorkspaceFilterModificationService

class WorkspaceFilterModificationServiceStub : IWorkspaceFilterModificationService {
    val inspectSelections = mutableListOf<VirtualFile>()
    val appliedPreviews = mutableListOf<FilterModificationPreview>()
    lateinit var inspectResult: FilterModificationPreview
    lateinit var applyResult: FilterModificationResult

    override fun inspect(selection: VirtualFile): FilterModificationPreview {
        inspectSelections.add(selection)
        return inspectResult
    }

    override fun apply(preview: FilterModificationPreview): FilterModificationResult {
        appliedPreviews.add(preview)
        return applyResult
    }
}
