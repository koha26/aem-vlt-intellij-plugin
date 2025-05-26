package com.kdiachenko.aem.filevault.integration.service

interface INotificationService {
    fun showInfo(title: String, content: String)

    fun showError(title: String, content: String)
}
