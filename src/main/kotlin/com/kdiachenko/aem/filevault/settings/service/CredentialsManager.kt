package com.kdiachenko.aem.filevault.settings.service

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service

@Service
class CredentialsManager {

    companion object {

        @JvmStatic
        fun getInstance(): CredentialsManager {
            return service<CredentialsManager>()
        }
    }

    fun get(id: String): Credentials? {
        val key = generateKey(id)
        return PasswordSafe.instance[key]
    }

    /*fun getAsync(id: String): Promise<Credentials?> {
        val key = generateKey(id)
        return PasswordSafe.instance.getAsync(key)
    }*/

    fun add(id: String, username: String, password: String) {
        val key = generateKey(id)
        val value = Credentials(username, password)
        PasswordSafe.instance[key] = value
    }

    fun remove(id: String) {
        val key = generateKey(id)
        PasswordSafe.instance[key] = null
    }

    private fun generateKey(id: String) =
        CredentialAttributes(
            serviceName = generateServiceName("aem-vlt-intellij-plugin-servers", id),
        )
}
