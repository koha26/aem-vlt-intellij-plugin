package com.kdiachenko.aem.filevault.settings

import com.intellij.openapi.components.*
import com.kdiachenko.aem.filevault.model.AEMServerConfig
import com.kdiachenko.aem.filevault.settings.service.CredentialsManager

/**
 * Persistent storage for AEM server configurations
 */
@State(
    name = "AEMServerSettings",
    storages = [Storage("aem-vlt-intellij-plugin-settings.xml")]
)
class AEMServerSettings : SimplePersistentStateComponent<AEMServerSettingsState>(AEMServerSettingsState()) {

    companion object {
        @JvmStatic
        fun getInstance(): AEMServerSettings {
            return service<AEMServerSettings>()
        }

        fun state() = getInstance().state
    }
}

class AEMServerSettingsState(
    var configuredServers: MutableList<AEMServerConfig> = mutableListOf()
) : BaseState() {

    private fun addDefaultConfig() {
        val defaultConfig = AEMServerConfig(
            name = "AEM Default Author",
            url = "http://localhost:4502",
            isDefault = true
        )
        configuredServers.add(defaultConfig)
        CredentialsManager.add(defaultConfig.id, "admin", "admin")
    }

    fun addServer(server: AEMServerConfig) {
        if (server.isDefault) {
            configuredServers.forEach { it.isDefault = false }
        }
        if (configuredServers.isEmpty()) {
            server.isDefault = true
        }
        configuredServers.add(server)
    }

    fun updateServer(updatedConfig: AEMServerConfig) {
        val index = configuredServers.indexOfFirst { it.id == updatedConfig.id }
        if (index != -1) {
            if (updatedConfig.isDefault) {
                configuredServers.forEach { it.isDefault = false }
            } else if (configuredServers.none { it.id != updatedConfig.id && it.isDefault }) {
                updatedConfig.isDefault = true
            }

            configuredServers[index] = updatedConfig
        }
    }

    fun removeServer(id: String) {
        val config = configuredServers.find { it.id == id }
        config?.let {
            val wasDefault = it.isDefault
            configuredServers.removeIf { cfg -> cfg.id == id }

            if (wasDefault && configuredServers.isNotEmpty()) {
                configuredServers.first().isDefault = true
            }

            CredentialsManager.remove(id)
        }
    }

    fun getDefaultServer(): AEMServerConfig? {
        return configuredServers.find { it.isDefault }
    }
}
