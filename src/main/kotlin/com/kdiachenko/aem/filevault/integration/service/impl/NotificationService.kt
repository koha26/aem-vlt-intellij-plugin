package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.kdiachenko.aem.filevault.integration.service.INotificationService

/**
 * Service for displaying notifications to users
 */
open class NotificationService(val project: Project) : INotificationService {

    companion object {
        const val GROUP_ID = "AEM VLT Notifications"

        @JvmStatic
        fun getInstance(project: Project): INotificationService {
            return project.getService(INotificationService::class.java)
        }
    }

    /**
     * Show information notification
     */
    override fun showInfo(title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, NotificationType.INFORMATION)
            .setTitle(title)
            .notify(project)
    }

    /**
     * Show error notification
     */
    override fun showError(title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, NotificationType.ERROR)
            .setTitle(title)
            .notify(project)
    }
}
