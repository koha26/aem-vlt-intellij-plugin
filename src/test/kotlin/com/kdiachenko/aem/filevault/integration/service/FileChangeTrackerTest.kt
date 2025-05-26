package com.kdiachenko.aem.filevault.integration.service

import com.kdiachenko.aem.filevault.integration.dto.OperationAction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FileChangeTrackerTest {

    @Test
    fun testInitialChangesIsEmpty() {
        val tracker = FileChangeTracker()
        assertEquals(0, tracker.changes.size)
    }

    @Test
    fun testAddSingleChange() {
        val tracker = FileChangeTracker()
        tracker.addChange(OperationAction.ADDED, "/path/to/file", "Added file")

        assertEquals(1, tracker.changes.size)
        assertEquals(OperationAction.ADDED, tracker.changes[0].action)
        assertEquals("/path/to/file", tracker.changes[0].path)
        assertEquals("Added file", tracker.changes[0].reason)
    }

    @Test
    fun testAddMultipleChanges() {
        val tracker = FileChangeTracker()
        tracker.addChange(OperationAction.ADDED, "/path/to/file", "Added file")
        tracker.addChange(OperationAction.UPDATED, "/path/to/another/file", "Updated file")

        assertEquals(2, tracker.changes.size)
        assertEquals(OperationAction.UPDATED, tracker.changes[1].action)
        assertEquals("/path/to/another/file", tracker.changes[1].path)
        assertEquals("Updated file", tracker.changes[1].reason)
    }

    @Test
    fun testFileChangeEntryProperties() {
        val entry = FileChangeEntry(
            action = OperationAction.DELETED,
            path = "/path/to/deleted/file",
            reason = "File deleted"
        )

        assertEquals(OperationAction.DELETED, entry.action)
        assertEquals("/path/to/deleted/file", entry.path)
        assertEquals("File deleted", entry.reason)
    }

    @Test
    fun testFileChangeEntryEquals() {
        val entry = FileChangeEntry(
            action = OperationAction.DELETED,
            path = "/path/to/deleted/file",
            reason = "File deleted"
        )

        val sameEntry = FileChangeEntry(
            action = OperationAction.DELETED,
            path = "/path/to/deleted/file",
            reason = "File deleted"
        )

        assertEquals(entry, sameEntry)
        assertEquals(entry.hashCode(), sameEntry.hashCode())
    }

    @Test
    fun testFileChangeEntryNotEquals() {
        val entry = FileChangeEntry(
            action = OperationAction.DELETED,
            path = "/path/to/deleted/file",
            reason = "File deleted"
        )

        val differentEntry = FileChangeEntry(
            action = OperationAction.ADDED,
            path = "/path/to/added/file",
            reason = "File added"
        )

        assert(entry != differentEntry)
    }
}
