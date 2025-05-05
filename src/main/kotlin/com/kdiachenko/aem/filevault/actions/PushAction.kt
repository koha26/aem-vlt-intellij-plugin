package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.kdiachenko.aem.filevault.service.FileVaultService
import com.kdiachenko.aem.filevault.service.NotificationService

/**
 * Action to push content to AEM repository
 */
class PushAction : BaseAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = getSelectedFile(e) ?: return
        val server = getSelectedServer(project) ?: return

        // Determine the JCR path
        val fileVaultService = FileVaultService.getInstance(project)
        val file = virtualToIoFile(virtualFile)
        val remotePath = showPathDialog(project, fileVaultService.getJcrPath(file))
            ?: return

        // Confirm before pushing
        val confirmation = Messages.showYesNoDialog(
            project,
            "Push ${file.name} to ${server.name} at path $remotePath?",
            "Confirm Push to AEM",
            "Push",
            "Cancel",
            Messages.getQuestionIcon()
        )

        if (confirmation != Messages.YES) {
            return
        }

        // Run the push operation in background
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Pushing to AEM", false) {
            override fun run(indicator: ProgressIndicator) {
                val result = fileVaultService.importContent(server, remotePath, file, indicator)
                var operationResult = result.get()

                // Show notification
                ApplicationManager.getApplication().invokeLater {
                    if (operationResult.success) {
                        NotificationService.showInfo(project, "Push Successful", operationResult.message)
                    } else {
                        NotificationService.showError(project, "Push Failed", operationResult.message)
                    }
                }
            }
        })
    }

    /**
     * Show dialog to confirm or modify remote path
     */
    private fun showPathDialog(project: Project, suggestedPath: String): String? {
        return Messages.showInputDialog(
            project,
            "Enter AEM repository path to push to:",
            "Push to AEM",
            null,
            suggestedPath,
            null
        )
    }
}
