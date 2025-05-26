package com.kdiachenko.aem.filevault.integration.dto

import com.kdiachenko.aem.filevault.integration.listener.OperationProgressTrackerListener
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VltOperationContextTest {

    @Test
    fun testVltOperationContextWithDefaultValues() {
        val serverConfig = DetailedAEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            isDefault = true,
            username = "admin",
            password = "admin"
        )

        val context = VltOperationContext(
            localAbsPath = "/path/to/local",
            serverConfig = serverConfig
        )

        assertEquals("/", context.jcrPath)
        assertEquals("/path/to/local", context.localAbsPath)
        assertEquals(serverConfig, context.serverConfig)
        assertNull(context.progressListener)
    }

    @Test
    fun testVltOperationContextWithCustomValues() {
        val serverConfig = DetailedAEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            isDefault = true,
            username = "admin",
            password = "admin"
        )

        val progressListener = OperationProgressTrackerListener()
        val context = VltOperationContext(
            jcrPath = "/content/test",
            localAbsPath = "/path/to/local",
            serverConfig = serverConfig,
            progressListener = progressListener
        )

        assertEquals("/content/test", context.jcrPath)
        assertEquals("/path/to/local", context.localAbsPath)
        assertEquals(serverConfig, context.serverConfig)
        assertEquals(progressListener, context.progressListener)
    }
}
