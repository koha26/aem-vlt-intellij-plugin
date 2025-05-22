package com.kdiachenko.aem.filevault.integration.factory.impl

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.impl.TestApplicationExtension
import com.kdiachenko.aem.filevault.integration.CustomizedVaultFsApp
import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith


class VaultAppFactoryTest {

    @Test
    fun testCreateVaultApp() {
        val serverConfig = DetailedAEMServerConfig(
            id = "test-id",
            name = "Test Server",
            url = "http://localhost:4502",
            isDefault = true,
            username = "admin",
            password = "admin"
        )

        val context = VltOperationContext(
            localAbsPath = System.getProperty("java.io.tmpdir"),
            serverConfig = serverConfig
        )

        val vaultApp = VaultAppFactory().createVaultApp(context)

        assertNotNull(vaultApp)
        assertEquals(CustomizedVaultFsApp::class.java, vaultApp.javaClass)
    }
}
