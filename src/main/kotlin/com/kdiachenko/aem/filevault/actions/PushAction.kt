package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.kdiachenko.aem.filevault.integration.facade.impl.FileVaultFacade
import com.kdiachenko.aem.filevault.integration.service.impl.NotificationService

/**
 * Action to push content to AEM repository
 */
class PushAction : BaseOperationAction() {
    private val logger = Logger.getInstance(PushAction::class.java)

    override fun getIcon() = com.intellij.icons.AllIcons.Vcs.Push

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = getSelectedFile(e) ?: return
        val server = getDefaultServer(project) ?: return

        val fileVaultService = FileVaultFacade.getInstance(project)
        val file = virtualToIoFile(virtualFile)

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Pushing to AEM", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                val result = fileVaultService.importContent(server, file, indicator)
                val operationResult = result.get()

                ApplicationManager.getApplication().invokeLater {
                    val notificationService = NotificationService.getInstance(project)
                    if (operationResult.success) {
                        operationResult.entries.forEach { logger.debug("Pushed: $it") }
                        notificationService.showInfo("Push Successful", operationResult.message)
                    } else {
                        notificationService.showError("Push Failed", operationResult.message)
                    }
                }
            }
        })
    }

}
