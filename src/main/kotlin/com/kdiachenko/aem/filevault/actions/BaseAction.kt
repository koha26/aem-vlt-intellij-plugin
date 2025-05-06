package com.kdiachenko.aem.filevault.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.kdiachenko.aem.filevault.model.AEMServer
import com.kdiachenko.aem.filevault.settings.AEMServerSettings
import java.io.File

/**
 * Base abstract class for FileVault actions
 */
abstract class BaseAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)

        // Enable action only if a project is opened and a file is selected
        e.presentation.isEnabledAndVisible = project != null && virtualFile != null
    }

    /**
     * Get a selected server for operation
     */
    protected fun getSelectedServer(project: Project): AEMServer? {
        val settings = AEMServerSettings.getInstance()
        val servers = settings.servers

        if (servers.isEmpty()) {
            Messages.showErrorDialog(
                project,
                "No AEM servers configured. Please add servers in Settings | AEM FileVault Settings.",
                "No Servers Configured"
            )
            return null
        }

        if (servers.size == 1) {
            return servers[0]
        }

        val defaultServer = settings.getDefaultServer()
        if (defaultServer != null) {
            return defaultServer
        }

        val serverNames = servers.map { it.name }.toTypedArray()
        val selection = Messages.showChooseDialog(
            project,
            "Select AEM server to connect to:",
            "Select Server",
            null,
            serverNames,
            serverNames[0]
        )

        return if (selection >= 0) servers[selection] else null
    }

    /**
     * Get the selected file from the action event
     */
    protected fun getSelectedFile(e: AnActionEvent): VirtualFile? {
        return e.getData(PlatformDataKeys.VIRTUAL_FILE)
    }

    /**
     * Convert VirtualFile to File
     */
    protected fun virtualToIoFile(virtualFile: VirtualFile): File {
        return File(virtualFile.path)
    }
}
