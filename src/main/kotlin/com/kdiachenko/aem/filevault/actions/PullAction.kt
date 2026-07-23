package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.kdiachenko.aem.filevault.integration.facade.impl.FileVaultFacade
import com.kdiachenko.aem.filevault.integration.filter.WorkspaceFilterOperationOptions
import com.kdiachenko.aem.filevault.integration.service.impl.NotificationService
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import javax.swing.Icon

/**
 * Action to pull content from AEM repository
 */
open class PullAction : BaseOperationAction() {
    private val logger = Logger.getInstance(PullAction::class.java)

    override fun getIcon(): Icon = com.intellij.icons.AllIcons.Vcs.Fetch

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = getSelectedFile(e) ?: return
        val server = getDefaultServer(project) ?: return

        preflight(project, virtualFile, "pull") { options ->
            executePull(project, virtualFile, server, options)
        }
    }

    private fun executePull(
        project: Project,
        virtualFile: VirtualFile,
        server: DetailedAEMServerConfig,
        options: WorkspaceFilterOperationOptions,
    ) {
        val fileVaultService = FileVaultFacade.getInstance(project)
        val file = virtualToIoFile(virtualFile)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Pulling from AEM", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.checkCanceled()
                indicator.isIndeterminate = false
                val completableFuture = fileVaultService.exportContent(server, file, indicator, options)
                val operationResult = completableFuture.get()

                ApplicationManager.getApplication().invokeLater {
                    val notificationService = NotificationService.getInstance(project)
                    if (operationResult.success) {
                        refreshVirtualFile(virtualFile)
                        operationResult.entries.forEach { logger.debug("Pulled: $it") }
                        notificationService.showInfo("Pull Successful", operationResult.message)
                    } else {
                        notificationService.showError("Pull Failed", operationResult.message)
                    }
                }
            }
        })
    }

    /**
     * Refresh the virtual file to show updated content
     */
    private fun refreshVirtualFile(file: VirtualFile) {
        file.refresh(true, true)
    }
}
