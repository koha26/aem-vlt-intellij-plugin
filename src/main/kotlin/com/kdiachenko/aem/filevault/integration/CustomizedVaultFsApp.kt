package com.kdiachenko.aem.filevault.integration

import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import org.apache.jackrabbit.vault.cli.VaultFsApp
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress
import java.io.File
import javax.jcr.SimpleCredentials

class CustomizedVaultFsApp(val aemServerConfig: DetailedAEMServerConfig) : VaultFsApp() {

    public override fun init() {
        super.init()


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
