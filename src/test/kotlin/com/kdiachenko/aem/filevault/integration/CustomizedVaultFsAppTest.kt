package com.kdiachenko.aem.filevault.integration

import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.listener.OperationProgressTrackerListener
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class CustomizedVaultFsAppTest {

    @Test
    fun testInitialization() {
        val serverConfig = DetailedAEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            isDefault = true,
            username = "admin",
            password = "admin"
        )

        val progressListener = OperationProgressTrackerListener()
        val localPath = System.getProperty("java.io.tmpdir")

        val context = VltOperationContext(
            jcrPath = "/content/test",
            localAbsPath = localPath,
            serverConfig = serverConfig,
            progressListener = progressListener
        )

        val app = CustomizedVaultFsApp(context)
        app.init()

        val credentialsStore = app.credentialsStore
        val credentials = credentialsStore.getCredentials(RepositoryAddress(serverConfig.url))

        assertNotNull(credentials)

        val file = app.getPlatformFile("test.txt", false)
        assertEquals(File(localPath, "test.txt"), file)
    }

    @Test
    fun testCredentialsSetup() {
        val serverConfig = DetailedAEMServerConfig(
            id = "test-id-2",
            name = "Test Server 2",
            url = "http://localhost:4503",
            isDefault = false,
            username = "author",
            password = "secret"
        )

        val context = VltOperationContext(
            localAbsPath = System.getProperty("java.io.tmpdir"),
            serverConfig = serverConfig
        )

        val app = CustomizedVaultFsApp(context)
        app.init()

        val defaultCreds = app.getProperty("conf.credentials")

        assertEquals("author:secret", defaultCreds)
    }

    @Test
    fun testInitializationError() {
        val serverConfig = DetailedAEMServerConfig(
            id = "test-id-2",
            name = "Test Server 2",
            url = "http://localhost:4503",
            isDefault = false,
            username = "author",
            password = "secret"
        )

        val context = VltOperationContext(
            localAbsPath = "/unknown/path/to/make/it/fail",
            serverConfig = serverConfig
        )

        val app = CustomizedVaultFsApp(context)
        app.init()
    }
}
