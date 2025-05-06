package com.kdiachenko.aem.filevault.integration

import com.kdiachenko.aem.filevault.model.AEMServer
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress
import java.io.File
import javax.jcr.SimpleCredentials

class CustomizedVaultFsApp(val aemServer: AEMServer) : VaultFsApp() {

    public override fun init() {
        super.init()

        val userPass = "${aemServer.username}:${aemServer.password}"
        setProperty(KEY_DEFAULT_CREDS, userPass)
        credentialsStore.storeCredentials(
            RepositoryAddress(aemServer.url),
            SimpleCredentials(aemServer.username, userPass.toCharArray())
        )
    }

    public override fun getPlatformFile(path: String?, mustExist: Boolean): File {
        return super.getPlatformFile(path, mustExist)
    }
}
