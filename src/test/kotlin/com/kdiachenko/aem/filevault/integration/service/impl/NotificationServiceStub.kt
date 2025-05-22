package com.kdiachenko.aem.filevault.integration.service.impl

import com.kdiachenko.aem.filevault.integration.service.INotificationService

class NotificationServiceStub : INotificationService {
    var info: List<NotificationEntry> = mutableListOf()
    var error: List<NotificationEntry> = mutableListOf()

    override fun showInfo(title: String, content: String) {
        info += NotificationEntry(title, content)
    }

    override fun showError(title: String, content: String) {
        error += NotificationEntry(title, content)
    }
}

data class NotificationEntry(val title: String, val content: String)
