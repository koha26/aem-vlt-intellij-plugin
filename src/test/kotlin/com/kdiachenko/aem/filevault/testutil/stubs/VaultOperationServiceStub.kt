package com.kdiachenko.aem.filevault.stubs

import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.service.IVaultOperationService

/**
 * Stub implementation of IVaultOperationService for testing
 */
open class VaultOperationServiceStub : IVaultOperationService {
    val exportCalls = mutableListOf<VltOperationContext>()
    val importCalls = mutableListOf<VltOperationContext>()
    var shouldThrowException = false

    // Customizable operation results
    var operationActions = listOf(
        Pair("A", "/path1"),  // Added
        Pair("U", "/path2"),  // Updated
        Pair("D", "/path3"),  // Deleted
        Pair("E", "/path4"),  // Error
        Pair("N", "/path5")   // Nothing changed
    )

    override fun export(context: VltOperationContext) {
        exportCalls.add(context)
        if (shouldThrowException) {
            throw RuntimeException("Test exception")
        }

        // Simulate progress messages with different action types
        operationActions.forEach { (action, path) ->
            context.progressListener?.onMessage(null, action, path)
        }
    }

    override fun import(context: VltOperationContext) {
        importCalls.add(context)
        if (shouldThrowException) {
            throw RuntimeException("Test exception")
        }

        // Simulate progress messages with different action types
        operationActions.forEach { (action, path) ->
            context.progressListener?.onMessage(null, action, path)
        }
    }
}
