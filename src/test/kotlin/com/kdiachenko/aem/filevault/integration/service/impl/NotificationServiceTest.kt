package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.util.concurrent.atomic.AtomicReference

class NotificationServiceTest : BasePlatformTestCase() {

    fun testShowInfo() {
        val shownNotification = AtomicReference<Notification>()
        with(project.messageBus.connect(testRootDisposable)) {
            subscribe(Notifications.TOPIC, object : Notifications {
                override fun notify(notification: Notification) {
                    if (notification.groupId != NotificationService.GROUP_ID) {
                        return
                    }
                    shownNotification.set(notification)
                }
            })
        }

        val notificationService = NotificationService.getInstance(project)
        notificationService.showInfo("Test title", "Test <b>content</b>")

        val notification = shownNotification.get()
        assertNotNull(notification)
        assertEquals("Test title", notification.title)
        assertEquals("Test <b>content</b>", notification.content)
        assertEquals(NotificationType.INFORMATION, notification.type)
    }

    fun testShowError() {
        val shownNotification = AtomicReference<Notification>()
        with(project.messageBus.connect(testRootDisposable)) {
            subscribe(Notifications.TOPIC, object : Notifications {
                override fun notify(notification: Notification) {
                    if (notification.groupId != NotificationService.GROUP_ID) {
                        return
                    }
                    shownNotification.set(notification)
                }
            })
        }

        val notificationService = NotificationService.getInstance(project)
        notificationService.showError("Test title", "Test <b>content</b>")

        val notification = shownNotification.get()
        assertNotNull(notification)
        assertEquals("Test title", notification.title)
        assertEquals("Test <b>content</b>", notification.content)
        assertEquals(NotificationType.ERROR, notification.type)
    }

}
