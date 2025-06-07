package com.kdiachenko.aem.filevault.integration

import com.kdiachenko.aem.filevault.integration.dto.VltOperationContext
import io.ktor.http.Url
import org.apache.jackrabbit.vault.cli.PlatformExecutionContext
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress
import org.apache.jackrabbit.vault.util.console.ExecutionException
import org.apache.jackrabbit.vault.vlt.ConfigCredentialsStore
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

        val url = Url(aemServerConfig.url)
        val repositoryUri = "${url.protocol.name}://${url.host}:${url.port}"
        val userPass = "${aemServerConfig.username}:${aemServerConfig.password}"
        setProperty(KEY_DEFAULT_CREDS, userPass)
        setStoreEnabled()
        credentialsStore.storeCredentials(
            RepositoryAddress(repositoryUri),
            SimpleCredentials(aemServerConfig.username, aemServerConfig.password.toCharArray())
        )
    }

    fun setStoreEnabled() {
        javaClass.superclass.getDeclaredField("confCredsProvider").apply {
            isAccessible = true
            val confCredsProvider = get(this@CustomizedVaultFsApp) as? ConfigCredentialsStore
            confCredsProvider?.setStoreEnabled(true)
        }
    }

    public override fun getPlatformFile(path: String?, mustExist: Boolean): File {
        return super.getPlatformFile(path, mustExist)
    }
}
