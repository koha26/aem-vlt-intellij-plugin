package com.kdiachenko.aem.filevault.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.kdiachenko.aem.filevault.model.AEMServer

/**
 * Persistent storage for AEM server configurations
 */
@State(
    name = "AEMServerSettings",
    storages = [Storage("aem-filevault.xml")]
)
class AEMServerSettings : PersistentStateComponent<AEMServerSettings> {
    var servers: MutableList<AEMServer> = mutableListOf()

    override fun getState(): AEMServerSettings = this

    override fun loadState(state: AEMServerSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun addServer(server: AEMServer) {
        // If this is set as default, unset others
        if (server.isDefault) {
            servers.forEach { it.isDefault = false }
        }
        servers.add(server)
    }

    fun updateServer(oldServer: AEMServer, newServer: AEMServer) {
        val index = servers.indexOf(oldServer)
        if (index >= 0) {
            // If this is set as default, unset others
            if (newServer.isDefault) {
                servers.forEach { it.isDefault = false }
            }
            servers[index] = newServer
        }
    }

    fun removeServer(server: AEMServer) {
        servers.remove(server)
    }

    fun getDefaultServer(): AEMServer? {
        return servers.find { it.isDefault }
    }

    companion object {
        @JvmStatic
        fun getInstance(): AEMServerSettings {
            return service<AEMServerSettings>()
        }
    }
}
