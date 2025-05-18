package com.kdiachenko.aem.filevault.integration.listener

import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener
import org.junit.Assert.assertEquals
import org.junit.Test

class OperationProgressTrackerListenerTest {

    @Test
    fun shouldRecordAddedOperation() {
        val listener = OperationProgressTrackerListener()
        listener.onMessage(ProgressTrackerListener.Mode.PATHS, "A", "/content/test/added")

        assertEquals(1, listener.entries.size)
        assertEquals(OperationAction.ADDED, listener.entries[0].action)
        assertEquals("/content/test/added", listener.entries[0].path)
        assertEquals("", listener.entries[0].message)
    }

    @Test
    fun shouldRecordUpdatedOperation() {
        val listener = OperationProgressTrackerListener()
        listener.onMessage(ProgressTrackerListener.Mode.PATHS, "U", "/content/test/updated")

        assertEquals(1, listener.entries.size)
        assertEquals(OperationAction.UPDATED, listener.entries[0].action)
        assertEquals("/content/test/updated", listener.entries[0].path)
        assertEquals("", listener.entries[0].message)
    }

    @Test
    fun shouldRecordDeletedOperation() {
        val listener = OperationProgressTrackerListener()
        listener.onMessage(ProgressTrackerListener.Mode.PATHS, "D", "/content/test/deleted")

        assertEquals(1, listener.entries.size)
        assertEquals(OperationAction.DELETED, listener.entries[0].action)
        assertEquals("/content/test/deleted", listener.entries[0].path)
        assertEquals("", listener.entries[0].message)
    }

    @Test
    fun shouldRecordErrorOperation() {
        val listener = OperationProgressTrackerListener()
        val exception = Exception("Test error message")

        listener.onError(ProgressTrackerListener.Mode.PATHS, "/content/test/error", exception)

        assertEquals(1, listener.entries.size)
        assertEquals(OperationAction.ERROR, listener.entries[0].action)
        assertEquals("/content/test/error", listener.entries[0].path)
        assertEquals("Test error message", listener.entries[0].message)
    }

    @Test
    fun shouldMaintainOperationOrder() {
        val listener = OperationProgressTrackerListener()

        listener.onMessage(ProgressTrackerListener.Mode.PATHS, "A", "/content/test/added")
        listener.onError(ProgressTrackerListener.Mode.PATHS, "/content/test/error1", Exception("Error 1"))
        listener.onMessage(ProgressTrackerListener.Mode.PATHS, "U", "/content/test/updated")
        listener.onError(ProgressTrackerListener.Mode.PATHS, "/content/test/error2", Exception("Error 2"))

        assertEquals(4, listener.entries.size)
        assertEquals(OperationAction.ADDED, listener.entries[0].action)
        assertEquals(OperationAction.ERROR, listener.entries[1].action)
        assertEquals(OperationAction.UPDATED, listener.entries[2].action)
        assertEquals(OperationAction.ERROR, listener.entries[3].action)
    }
}
