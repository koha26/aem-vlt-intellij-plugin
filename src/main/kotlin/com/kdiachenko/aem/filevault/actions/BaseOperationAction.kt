package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.kdiachenko.aem.filevault.i18n.Messages as BundleMessages
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterOperationOptions
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterStatus
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterValidationResult
import com.kdiachenko.aem.filevault.integration.service.FilterModificationPreview
import com.kdiachenko.aem.filevault.integration.service.FilterModificationResult
import com.kdiachenko.aem.filevault.integration.service.FilterModificationStatus
import com.kdiachenko.aem.filevault.integration.service.impl.NotificationService
import com.kdiachenko.aem.filevault.integration.service.impl.WorkspaceFilterModificationService
import com.kdiachenko.aem.filevault.integration.service.impl.WorkspaceFilterService
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import com.kdiachenko.aem.filevault.model.toDetailed
import com.kdiachenko.aem.filevault.settings.AEMServerSettings
import com.kdiachenko.aem.filevault.util.JcrPathUtil.resolveContentPackage
import java.io.File
import java.nio.file.Path
import javax.swing.Icon

/**
 * Base abstract class for FileVault actions
 */
abstract class BaseOperationAction : AnAction() {

    abstract fun getIcon(): Icon

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = getSelectedFile(e)

        e.presentation.isEnabledAndVisible = project != null && virtualFile != null && virtualFile.inUnderJcrRoot()
        e.presentation.icon = getIcon()
    }

    fun VirtualFile.inUnderJcrRoot(): Boolean = Path.of(path).resolveContentPackage() != null

    /**
     * Get a selected server for operation
     */
    protected open fun getDefaultServer(project: Project): DetailedAEMServerConfig? {
        val settings = AEMServerSettings.getInstance().state
        val servers = settings.configuredServers

        if (servers.isEmpty()) {
            Messages.showErrorDialog(
                project,
                "No AEM servers configured. Please add servers in Settings | AEM VLT Settings.",
                "No Servers Configured"
            )
            return null
        }

        if (servers.size == 1) {
            return servers[0].toDetailed()
        }

        val defaultServer = settings.getDefaultServer()
        if (defaultServer != null) {
            return defaultServer.toDetailed()
        }

        Messages.showErrorDialog(
            project,
            "No default AEM servers configured. Please mark desired server as default in Settings | AEM VLT Settings.",
            "No Default Servers Configured"
        )
        return null
    }

    /**
     * Get the selected file from the action event
     */
    protected fun getSelectedFile(e: AnActionEvent): VirtualFile? {
        return e.getData(PlatformDataKeys.VIRTUAL_FILE)
    }

    protected fun virtualToIoFile(virtualFile: VirtualFile): File {
        return File(virtualFile.path)
    }

    protected fun preflight(
        project: Project,
        file: VirtualFile,
        operationName: String,
        onApproved: (WorkspaceFilterOperationOptions) -> Unit,
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Validating $operationName scope", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.checkCanceled()
                val validation = WorkspaceFilterService.getInstance().evaluate(Path.of(file.path))
                val preview = if (validation.status in setOf(
                        WorkspaceFilterStatus.FILTER_NOT_FOUND,
                        WorkspaceFilterStatus.FILTER_EMPTY,
                        WorkspaceFilterStatus.OUTSIDE_FILTER,
                        WorkspaceFilterStatus.EXCLUDED,
                    )
                ) {
                    WorkspaceFilterModificationService.getInstance(project).inspect(file)
                } else {
                    null
                }
                ApplicationManager.getApplication().invokeLater {
                    if (project.isDisposed) {
                        return@invokeLater
                    }
                    when (validation.status) {
                        WorkspaceFilterStatus.FULLY_INCLUDED -> onApproved(
                            WorkspaceFilterOperationOptions(expectedFilterFingerprint = validation.filterFingerprint),
                        )

                        WorkspaceFilterStatus.PARTIALLY_INCLUDED -> {
                            val choice = Messages.showDialog(
                                project,
                                validation.message,
                                BundleMessages.message("workspace.filter.partial.title"),
                                arrayOf(
                                    BundleMessages.message("workspace.filter.partial.continue"),
                                    BundleMessages.message("common.cancel"),
                                ),
                                1,
                                Messages.getWarningIcon(),
                            )
                            if (choice == 0) {
                                onApproved(
                                    WorkspaceFilterOperationOptions(
                                        partialScopeApproved = true,
                                        expectedFilterFingerprint = validation.filterFingerprint,
                                    ),
                                )
                            }
                        }

                        else -> showBlockedFilterDialog(project, validation, preview)
                    }
                }
            }
        })
    }

    private fun showBlockedFilterDialog(
        project: Project,
        result: WorkspaceFilterValidationResult,
        preview: FilterModificationPreview?,
    ) {
        when (result.status) {
            WorkspaceFilterStatus.FILTER_INVALID -> {
                val choice = Messages.showDialog(
                    project,
                    result.message,
                    BundleMessages.message("workspace.filter.invalid.title"),
                    arrayOf(
                        BundleMessages.message("workspace.filter.invalid.open"),
                        BundleMessages.message("common.cancel"),
                    ),
                    1,
                    Messages.getErrorIcon(),
                )
                if (choice == 0) {
                    result.filterFile?.let { openFile(project, it) }
                }
            }

            WorkspaceFilterStatus.FILTER_CHANGED -> Messages.showInfoMessage(
                project,
                result.message,
                BundleMessages.message("workspace.filter.changed.title"),
            )

            WorkspaceFilterStatus.FILTER_NOT_FOUND,
            WorkspaceFilterStatus.FILTER_EMPTY,
            WorkspaceFilterStatus.OUTSIDE_FILTER,
            WorkspaceFilterStatus.EXCLUDED -> {
                val choice = Messages.showDialog(
                    project,
                    result.message + "\n" + BundleMessages.message("workspace.filter.blocked.hint"),
                    BundleMessages.message("filter.blocked.title"),
                    arrayOf(
                        BundleMessages.message("action.addToFilter.text"),
                        BundleMessages.message("common.cancel"),
                    ),
                    1,
                    Messages.getWarningIcon(),
                )
                if (choice == 0 && preview != null) {
                    notifyFilterResult(project, WorkspaceFilterModificationService.getInstance(project).apply(preview))
                }
            }

            WorkspaceFilterStatus.NOT_IN_CONTENT_PACKAGE -> Messages.showErrorDialog(
                project,
                result.message,
                BundleMessages.message("workspace.filter.selection.invalid.title"),
            )

            WorkspaceFilterStatus.FULLY_INCLUDED,
            WorkspaceFilterStatus.PARTIALLY_INCLUDED -> error("Executable status passed to blocked dialog: ${result.status}")
        }
    }

    private fun openFile(project: Project, path: Path) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path) ?: return
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }

    private fun notifyFilterResult(project: Project, result: FilterModificationResult) {
        if (result.status == FilterModificationStatus.ADDED) {
            result.filterFile?.let { file ->
                file.refresh(false, false)
                FileEditorManager.getInstance(project).openFile(file, true)
            }
            NotificationService.getInstance(project).showInfo("FileVault Filter Updated", result.message)
        } else {
            NotificationService.getInstance(project).showError("FileVault Filter Not Changed", result.message)
        }
    }
}
