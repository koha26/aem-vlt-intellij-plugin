package com.kdiachenko.aem.filevault.integration

import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import org.apache.jackrabbit.vault.cli.PlatformExecutionContext
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress
import org.apache.jackrabbit.vault.util.console.ExecutionException
import java.io.File
import java.io.IOException
import javax.jcr.SimpleCredentials

class CustomizedVaultFsApp(val context: VltOperationContext) : VaultFsApp() {

    public override fun init() {
        super.init()

        val cwd: File?
        try {
            cwd = File(context.localAbsPath).getCanonicalFile()
        } catch (e: IOException) {
            throw ExecutionException(e)
        }
        val ctxPlatform = PlatformExecutionContext(this, "local", cwd)
        console.removeContext(ctxPlatform)
        console.addContext(ctxPlatform)
        javaClass.superclass.getDeclaredField("ctxPlatform").apply {
            isAccessible = true
            set(this@CustomizedVaultFsApp, ctxPlatform)
        }

        val aemServerConfig = context.serverConfig
        val userPass = "${aemServerConfig.username}:${aemServerConfig.password}"
        setProperty(KEY_DEFAULT_CREDS, userPass)
        credentialsStore.storeCredentials(
            RepositoryAddress(aemServerConfig.url),
            SimpleCredentials(aemServerConfig.username, userPass.toCharArray())
        )
    }

    public override fun getPlatformFile(path: String?, mustExist: Boolean): File {
        return super.getPlatformFile(path, mustExist)
    }
}
