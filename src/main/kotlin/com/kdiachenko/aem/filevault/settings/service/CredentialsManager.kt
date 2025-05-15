package com.kdiachenko.aem.filevault.settings.service

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import org.jetbrains.concurrency.Promise

object CredentialsManager {

    private val passwordSafe = PasswordSafe.Companion.instance

    fun get(id: String): Credentials? {
        val key = generateKey(id)
        return passwordSafe.get(key)
    }

    fun getAsync(id: String): Promise<Credentials?> {
        val key = generateKey(id)
        return passwordSafe.getAsync(key)
    }

    fun add(id: String, username: String, password: String) {
        val key = generateKey(id)
        val value = Credentials(username, password)
        passwordSafe.set(key, value)
    }

    fun remove(id: String) {
        val key = generateKey(id)
        passwordSafe.set(key, null)
    }

    private fun generateKey(id: String) =
        CredentialAttributes(generateServiceName("aem-vlt-intellij-plugin-servers", id))
}
