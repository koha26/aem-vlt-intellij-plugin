package com.kdiachenko.aem.filevault.integration.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OperationEntryDetailTest {

    @Test
    fun testOperationEntryDetailWithDefaultMessage() {
        val entry = OperationEntryDetail(
            action = OperationAction.ADDED,
            path = "/content/test"
        )

        assertEquals(OperationAction.ADDED, entry.action)
        assertEquals("/content/test", entry.path)
        assertEquals("", entry.message)
    }

    @Test
    fun testOperationEntryDetailWithCustomMessage() {
        val entry = OperationEntryDetail(
            action = OperationAction.ERROR,
            path = "/content/test",
            message = "Error message"
        )

        assertEquals(OperationAction.ERROR, entry.action)
        assertEquals("/content/test", entry.path)
        assertEquals("Error message", entry.message)
    }

    @Test
    fun testOperationEntryDetailEqualityWithSameProperties() {
        val entry1 = OperationEntryDetail(
            action = OperationAction.UPDATED,
            path = "/content/test",
            message = "Updated"
        )

        val entry2 = OperationEntryDetail(
            action = OperationAction.UPDATED,
            path = "/content/test",
            message = "Updated"
        )

        assertEquals(entry1, entry2)
        assertEquals(entry1.hashCode(), entry2.hashCode())
    }

    @Test
    fun testOperationEntryDetailEqualityWithDifferentProperties() {
        val entry1 = OperationEntryDetail(
            action = OperationAction.UPDATED,
            path = "/content/test",
            message = "Updated"
        )

        val entry2 = OperationEntryDetail(
            action = OperationAction.DELETED,
            path = "/content/test",
            message = "Deleted"
        )

        assert(entry1 != entry2)
    }
}
