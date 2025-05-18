package com.kdiachenko.aem.filevault.settings

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.kdiachenko.aem.filevault.model.AEMServerConfig

class AEMServerSettingsTest : BasePlatformTestCase() {

    private lateinit var settingsState: AEMServerSettingsState

    public override fun setUp() {
        super.setUp()

        settingsState = AEMServerSettingsState()
    }

    fun testAddServer() {
        val server = AEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            isDefault = false
        )

        settingsState.addServer(server)

        assertEquals(1, settingsState.configuredServers.size)
        assertEquals(server, settingsState.configuredServers[0])
    }

    fun testAddServer_firstServerBecomesDefault() {
        val server = AEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            isDefault = false
        )

        settingsState.addServer(server)

        assertEquals(1, settingsState.configuredServers.size)
        assertTrue(settingsState.configuredServers[0].isDefault)
    }

    fun testAddServer_defaultServerMakesOthersNonDefault() {
        val server1 = AEMServerConfig(
            id = "server1-id",
            name = "Server 1",
            url = "http://localhost:4502",
            isDefault = true
        )

        settingsState.addServer(server1)

        val server2 = AEMServerConfig(
            id = "server2-id",
            name = "Server 2",
            url = "http://localhost:4503",
            isDefault = true
        )

        settingsState.addServer(server2)

        assertEquals(2, settingsState.configuredServers.size)
        assertFalse(settingsState.configuredServers[0].isDefault)
        assertTrue(settingsState.configuredServers[1].isDefault)
    }

    fun testClearConfiguredServers() {
        settingsState.addServer(AEMServerConfig(
            id = "server1-id",
            name = "Server 1",
            url = "http://localhost:4502",
            isDefault = true
        ))

        settingsState.addServer(AEMServerConfig(
            id = "server2-id",
            name = "Server 2",
            url = "http://localhost:4503",
            isDefault = false
        ))

        assertEquals(2, settingsState.configuredServers.size)
        settingsState.clearConfiguredServers()
        assertEquals(0, settingsState.configuredServers.size)
    }

    fun testGetDefaultServer() {
        settingsState.addServer(AEMServerConfig(
            id = "server1-id",
            name = "Server 1",
            url = "http://localhost:4502",
            isDefault = false
        ))

        settingsState.addServer(AEMServerConfig(
            id = "server2-id",
            name = "Server 2",
            url = "http://localhost:4503",
            isDefault = true
        ))

        val defaultServer = settingsState.getDefaultServer()

        assertNotNull(defaultServer)
        assertEquals("server2-id", defaultServer?.id)
        assertEquals("Server 2", defaultServer?.name)
        assertEquals("http://localhost:4503", defaultServer?.url)
        assertTrue(defaultServer?.isDefault ?: false)
    }

    fun testGetDefaultServer_whenNoDefaultServer() {
        settingsState.clearConfiguredServers()

        val defaultServer = settingsState.getDefaultServer()

        assertNull(defaultServer)
    }
}
