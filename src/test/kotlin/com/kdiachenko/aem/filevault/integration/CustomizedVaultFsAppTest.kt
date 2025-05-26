package com.kdiachenko.aem.filevault.integration

import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.integration.listener.OperationProgressTrackerListener
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class CustomizedVaultFsAppTest {

    @Test
    fun testInitialization() {
        val serverConfig = DetailedAEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "https://localhost:4502",
            isDefault = true,
            username = "admin",
            password = "admin"
        )

        val progressListener = OperationProgressTrackerListener()
        val tempFolder = createTempDirectory("test-vault-fs-app-").toFile()
        val localPath = tempFolder.absolutePath

        val context = VltOperationContext(
            jcrPath = "/content/test",
            localAbsPath = localPath,
            serverConfig = serverConfig,
            progressListener = progressListener
        )

        val app = CustomizedVaultFsApp(context)
        app.init()

        val file = app.getPlatformFile("test.txt", false)
        assertEquals(File(localPath, "test.txt").absolutePath, file.absolutePath)
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
}
