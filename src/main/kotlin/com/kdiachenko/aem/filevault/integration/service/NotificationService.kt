package com.kdiachenko.aem.filevault.integration.service

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * Service for displaying notifications to users
 */
object NotificationService {
    private const val GROUP_ID = "AEM FileVault Notifications"

    /**
     * Show information notification
     */
    fun showInfo(project: Project?, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, NotificationType.INFORMATION)
            .setTitle(title)
            .notify(project)
    }

    /**
     * Show error notification
     */
    fun showError(project: Project?, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, NotificationType.ERROR)
            .setTitle(title)
            .notify(project)
    }

    /**
     * Show warning notification
     */
    fun showWarning(project: Project?, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(content, NotificationType.WARNING)
            .setTitle(title)
            .notify(project)
    }
}
