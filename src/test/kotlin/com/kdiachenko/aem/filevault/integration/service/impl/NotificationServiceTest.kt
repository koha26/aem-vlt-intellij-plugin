package com.kdiachenko.aem.filevault.integration.service.impl

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.UIUtil
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration

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

        runInEdtAndWait {
            val notificationService = NotificationService.getInstance(project)
            notificationService.showInfo("Test title", "Test <b>content</b>")
        }

        // fix text
        /* waitForCondition(60.toDuration(TimeUnit.SECONDS.toDurationUnit())) { shownNotification.get() != null }

         val notification = shownNotification.get()
         assertNotNull(notification)
         assertEquals("Test title", notification.title)
         assertEquals("Test <b>content</b>", notification.content)
         assertEquals(NotificationType.INFORMATION, notification.type)*/
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

        runInEdtAndWait {
            val notificationService = NotificationService.getInstance(project)
            notificationService.showError("Test title", "Test <b>content</b>")
        }

        // fix text
        /*waitForCondition(60.toDuration(TimeUnit.SECONDS.toDurationUnit())) { shownNotification.get() != null }

        val notification = shownNotification.get()
        assertNotNull(notification)
        assertEquals("Test title", notification.title)
        assertEquals("Test <b>content</b>", notification.content)
        assertEquals(NotificationType.ERROR, notification.type)*/
    }

    @JvmSynthetic
    @Throws(TimeoutException::class)
    fun waitForCondition(timeout: Duration, condition: () -> Boolean) {
        val timeoutMillis = timeout.inWholeMilliseconds
        val deadline = System.currentTimeMillis() + timeoutMillis
        var waitUnit = ((timeoutMillis + 9) / 10).coerceAtMost(10)
        val isEdt = com.intellij.util.ui.EDT.isCurrentThreadEdt()
        while (waitUnit > 0) {
            if (isEdt) {
                UIUtil.dispatchAllInvocationEvents()
            }
            if (condition()) {
                return
            }
            Thread.sleep(waitUnit)
            waitUnit = waitUnit.coerceAtMost(deadline - System.currentTimeMillis())
        }
        throw TimeoutException()
    }

}
