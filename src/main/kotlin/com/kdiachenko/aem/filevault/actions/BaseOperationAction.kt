package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.messages.MessagesService
import com.intellij.openapi.vfs.VirtualFile
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import com.kdiachenko.aem.filevault.model.toDetailed
import com.kdiachenko.aem.filevault.settings.AEMServerSettings
import com.kdiachenko.aem.filevault.util.JcrPathUtil.toJcrPath
import java.awt.Component
import java.io.File
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

    fun VirtualFile.inUnderJcrRoot(): Boolean = this.toJcrPath() != null

    /**
     * Get a selected server for operation
     */
    protected open fun getDefaultServer(project: Project): DetailedAEMServerConfig? {
        val settings = AEMServerSettings.getInstance().state
        val servers = settings.configuredServers

        if (servers.isEmpty()) {
            Messages.showErrorDialog(
                project,
                "No AEM servers configured. Please add servers in Settings | AEM FileVault Settings.",
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
            "No default AEM servers configured. Please mark desired server as default in Settings | AEM FileVault Settings.",
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
}
