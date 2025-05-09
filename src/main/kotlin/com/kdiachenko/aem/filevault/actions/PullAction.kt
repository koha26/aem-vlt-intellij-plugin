package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import com.kdiachenko.aem.filevault.integration.service.impl.FileVaultService
import com.kdiachenko.aem.filevault.integration.service.NotificationService
import com.kdiachenko.aem.filevault.util.JcrPathUtil
import javax.swing.Icon

/**
 * Action to pull content from AEM repository
 */
class PullAction : BaseAction() {
    private val logger = Logger.getInstance(FileVaultService::class.java)

    override fun getIcon(): Icon = com.intellij.icons.AllIcons.Vcs.Fetch

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = getSelectedFile(e) ?: return
        val server = getSelectedServer(project) ?: return

        // Determine the JCR path
        val fileVaultService = FileVaultService.getInstance(project)
        val file = virtualToIoFile(virtualFile)
        val remotePath = JcrPathUtil.calculateJcrPath(file)

        // Run the pull operation in background
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Pulling from AEM", false) {
            override fun run(indicator: ProgressIndicator) {
                val completableFuture = fileVaultService.exportContent(server, remotePath, file, indicator)
                val operationResult = completableFuture.get()

                // Show notification and refresh VFS
                ApplicationManager.getApplication().invokeLater {
                    if (operationResult.success) {
                        refreshVirtualFile(virtualFile)
                        operationResult.entries.forEach {
                            logger.info("$it")
                        }
                        NotificationService.showInfo(project, "Pull Successful", operationResult.message)
                    } else {
                        NotificationService.showError(project, "Pull Failed", operationResult.message)
                    }
                }
            }
        })
    }

    /**
     * Show a dialog to confirm or modify remote path
     */
    private fun showPathDialog(project: Project, suggestedPath: String): String? {
        return Messages.showInputDialog(
            project,
            "Enter AEM repository path to pull from:",
            "Pull from AEM",
            null,
            suggestedPath,
            null
        )
    }

    /**
     * Refresh the virtual file to show updated content
     */
    private fun refreshVirtualFile(file: VirtualFile) {
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                file.refresh(false, true)

                // If it's a directory, refresh all children recursively
                if (file.isDirectory) {
                    VfsUtil.markDirtyAndRefresh(false, true, true, file)
                }
            }
        }
    }
}
