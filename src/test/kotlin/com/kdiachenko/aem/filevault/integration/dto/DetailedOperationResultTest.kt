package com.kdiachenko.aem.filevault.integration.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DetailedOperationResultTest {

    @Test
    fun testSuccessfulResultWithNoEntries() {
        val successResult = DetailedOperationResult(
            success = true,
            message = "Operation successful",
            entries = emptyList()
        )

        assertTrue(successResult.success)
        assertEquals("Operation successful", successResult.message)
        assertEquals(0, successResult.entries.size)
    }

    @Test
    fun testFailedResultWithNoEntries() {
        val failedResult = DetailedOperationResult(
            success = false,
            message = "Operation failed",
            entries = emptyList()
        )

        assertFalse(failedResult.success)
        assertEquals("Operation failed", failedResult.message)
        assertEquals(0, failedResult.entries.size)
    }

    @Test
    fun testSuccessfulResultWithEntries() {
        val entries = listOf(
            OperationEntryDetail(
                action = OperationAction.ADDED,
                path = "/content/test/added",
                message = "Added file"
            ),
            OperationEntryDetail(
                action = OperationAction.UPDATED,
                path = "/content/test/updated",
                message = "Updated file"
            )
        )

        val resultWithEntries = DetailedOperationResult(
            success = true,
            message = "Operation with entries",
            entries = entries
        )

        assertTrue(resultWithEntries.success)
        assertEquals("Operation with entries", resultWithEntries.message)
        assertEquals(2, resultWithEntries.entries.size)
        assertEquals(OperationAction.ADDED, resultWithEntries.entries[0].action)
        assertEquals("/content/test/added", resultWithEntries.entries[0].path)
        assertEquals("Added file", resultWithEntries.entries[0].message)
        assertEquals(OperationAction.UPDATED, resultWithEntries.entries[1].action)
        assertEquals("/content/test/updated", resultWithEntries.entries[1].path)
        assertEquals("Updated file", resultWithEntries.entries[1].message)
    }

    @Test
    fun testDetailedOperationResultEqualityForIdenticalObjects() {
        val entries = listOf(
            OperationEntryDetail(
                action = OperationAction.ADDED,
                path = "/content/test/added",
                message = "Added file"
            )
        )

        val result1 = DetailedOperationResult(
            success = true,
            message = "Operation successful",
            entries = entries
        )

        val result2 = DetailedOperationResult(
            success = true,
            message = "Operation successful",
            entries = entries
        )

        assertEquals(result1, result2)
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun testDetailedOperationResultEqualityForDifferentObjects() {
        val entries = listOf(
            OperationEntryDetail(
                action = OperationAction.ADDED,
                path = "/content/test/added",
                message = "Added file"
            )
        )

        val result1 = DetailedOperationResult(
            success = true,
            message = "Operation successful",
            entries = entries
        )

        val result3 = DetailedOperationResult(
            success = false,
            message = "Operation failed",
            entries = entries
        )

        assertTrue(result1 != result3)
    }
}
