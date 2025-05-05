package com.kdiachenko.aem.filevault.service

import com.kdiachenko.aem.filevault.model.AEMServer
import org.apache.jackrabbit.vault.cli.VaultFsApp
import java.io.File

class CustomizedVaultFsApp(val aemServer: AEMServer): VaultFsApp() {

    public override fun init() {
        super.init()

        val userPass = "${aemServer.username}:${aemServer.password}"
        setProperty(KEY_DEFAULT_CREDS, userPass)
    }

    public override fun getPlatformFile(path: String?, mustExist: Boolean): File {
        return super.getPlatformFile(path, mustExist)
    }
}
