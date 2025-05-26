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
        CredentialsManager.getInstance().add(defaultConfig.id, "admin", "admin")
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

    fun clearConfiguredServers() {
        configuredServers.clear()
    }

    fun getDefaultServer(): AEMServerConfig? {
        return configuredServers.find { it.isDefault }
    }
}
