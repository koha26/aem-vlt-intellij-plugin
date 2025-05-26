package com.kdiachenko.aem.filevault.integration.dto

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class OperationActionTest {

    companion object {
        @JvmStatic
        fun operationActionData(): Collection<Array<Any>> {
            return listOf(
                arrayOf(OperationAction.ADDED, "A"),
                arrayOf(OperationAction.UPDATED, "U"),
                arrayOf(OperationAction.DELETED, "D"),
                arrayOf(OperationAction.ERROR, "E"),
                arrayOf(OperationAction.NOTHING_CHANGED, ""),
                arrayOf(OperationAction.NOTHING_CHANGED, "X"),
                arrayOf(OperationAction.NOTHING_CHANGED, "INVALID")
            )
        }
    }

    @ParameterizedTest(name = "Should return {0} for input {1}")
    @MethodSource("operationActionData")
    fun testFromString(expected: OperationAction, input: String) {
        Assertions.assertEquals(expected, OperationAction.fromString(input))
    }
}
