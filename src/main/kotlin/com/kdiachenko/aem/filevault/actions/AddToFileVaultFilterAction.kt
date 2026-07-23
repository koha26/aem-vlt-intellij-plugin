package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.kdiachenko.aem.filevault.i18n.Messages as BundleMessages
import com.kdiachenko.aem.filevault.integration.service.FilterModificationPreview
import com.kdiachenko.aem.filevault.integration.service.FilterModificationResult
import com.kdiachenko.aem.filevault.integration.service.FilterModificationStatus
import com.kdiachenko.aem.filevault.integration.service.impl.NotificationService
import com.kdiachenko.aem.filevault.integration.service.impl.WorkspaceFilterModificationService
import com.kdiachenko.aem.filevault.util.JcrPathUtil.resolveContentPackage
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toNormalizedJcrPath
import java.nio.file.Path

class AddToFileVaultFilterAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val path = file?.let { Path.of(it.path) }
        val context = path?.resolveContentPackage()
        e.presentation.text = BundleMessages.message("action.addToFilter.text")
        e.presentation.isEnabledAndVisible = e.project != null &&
            file != null &&
            context != null &&
            path.toNormalizedJcrPath(context) != "/"
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val selection = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val service = WorkspaceFilterModificationService.getInstance(project)
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Inspecting FileVault filter", true) {
            override fun run(indicator: ProgressIndicator) {
                val preview = service.inspect(selection)
                indicator.checkCanceled()
                ApplicationManager.getApplication().invokeLater {
                    when (preview.status) {
                        FilterModificationStatus.ADDED -> notifyResult(project, service.apply(preview))
                        FilterModificationStatus.EXPANSION_CONFIRMATION_REQUIRED -> {
                            val choice = Messages.showDialog(
                                project,
                                preview.message,
                                BundleMessages.message("filter.partial.title"),
                                arrayOf(
                                    BundleMessages.message("action.addToFilter.text"),
                                    BundleMessages.message("common.cancel"),
                                ),
                                1,
                                Messages.getWarningIcon(),
                            )
                            if (choice == 0) {
                                notifyResult(project, service.apply(preview))
                            }
                        }

                        FilterModificationStatus.ALREADY_COVERED,
                        FilterModificationStatus.EXACT_ROOT_EXISTS -> NotificationService.getInstance(project)
                            .showInfo("FileVault Filter Unchanged", preview.message)

                        FilterModificationStatus.FILTER_INVALID -> showInvalidFilterDialog(project, preview)

                        FilterModificationStatus.FILTER_CHANGED,
                        FilterModificationStatus.NOT_APPLICABLE -> NotificationService.getInstance(project)
                            .showError("FileVault Filter Not Changed", preview.message)
                    }
                }
            }
        })
    }

    private fun showInvalidFilterDialog(project: com.intellij.openapi.project.Project, preview: FilterModificationPreview) {
        val service = WorkspaceFilterModificationService.getInstance(project) as WorkspaceFilterModificationService
        val choice = Messages.showDialog(
            project,
            preview.message,
            BundleMessages.message("filter.invalid.title"),
            arrayOf(
                BundleMessages.message("workspace.filter.invalid.open"),
                BundleMessages.message("common.cancel"),
            ),
            1,
            Messages.getErrorIcon(),
        )
        if (choice == 0) {
            preview.context?.filterFile?.let(service::openFile)
        }
    }

    private fun notifyResult(project: com.intellij.openapi.project.Project, result: FilterModificationResult) {
        if (result.status == FilterModificationStatus.ADDED) {
            result.filterFile?.let { file ->
                file.refresh(false, false)
                com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(file, true)
            }
            NotificationService.getInstance(project).showInfo("FileVault Filter Updated", result.message)
        } else {
            NotificationService.getInstance(project).showError("FileVault Filter Not Changed", result.message)
        }
    }
}
