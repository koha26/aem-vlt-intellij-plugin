package com.kdiachenko.aem.filevault.settings.ui

import com.intellij.util.Function
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.table.TableModelEditor
import com.kdiachenko.aem.filevault.model.AEMServerConfig
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import com.kdiachenko.aem.filevault.model.toDetailed
import com.kdiachenko.aem.filevault.settings.AEMServerSettings
import com.kdiachenko.aem.filevault.settings.service.CredentialsManager
import com.kdiachenko.aem.filevault.settings.SettingsPanel
import java.util.UUID
import javax.swing.JPanel

class AEMServerSettingsPanel() : SettingsPanel {
    private var initialConfigurations = state().configuredServers.toDetailed()
    private lateinit var tableModelEditor: TableModelEditor<DetailedAEMServerConfig>

    override fun name(): String = "AEM FileVault Settings"

    override fun getPanel(): JPanel {
        tableModelEditor = TableModelEditor(
            initialConfigurations.toMutableCopy(),
            createColumnInfos(),
            AEMServerEditor(),
            "Configure AEM servers"
        )
        tableModelEditor.enabled(true)
        tableModelEditor.setShowGrid(false)
        return tableModelEditor.createComponent() as JPanel
    }

    override fun isModified(): Boolean {
        return tableModelEditor.isModified
    }

    override fun save() {
        initialConfigurations.forEach { CredentialsManager.remove(it.id) }

        val newAEMServers = tableModelEditor.apply()
        val newState = state()
        newAEMServers.forEach {
            CredentialsManager.add(it.id, it.username, it.password)
            newState.configuredServers.add(AEMServerConfig(it.id, it.name, it.url))
        }

        AEMServerSettings.Companion.getInstance().loadState(newState)

        initialConfigurations = newState.configuredServers.toDetailed()
    }

    override fun reset() {
        tableModelEditor.reset(initialConfigurations)
    }

    private fun List<DetailedAEMServerConfig>.toMutableCopy() =
        this.map { it.copy() }.toMutableList()

    private fun state() = AEMServerSettings.state()

    private fun createColumnInfos()
            : Array<ColumnInfo<DetailedAEMServerConfig, *>> {
        return arrayOf(
            createColumnInfo("Server Name") { it.name },
            createColumnInfo("URL") { it.url },
            createColumnInfo("Username") { it.username }
        )
    }

    private fun <T> createColumnInfo(
        name: String,
        getter: (config: DetailedAEMServerConfig) -> T
    ): ColumnInfo<DetailedAEMServerConfig, T> {
        return object : ColumnInfo<DetailedAEMServerConfig, T>(name) {
            override fun valueOf(config: DetailedAEMServerConfig): T = getter(config)
        }
    }

    inner class AEMServerEditor : TableModelEditor.DialogItemEditor<DetailedAEMServerConfig> {
        override fun getItemClass(): Class<out DetailedAEMServerConfig> {
            return DetailedAEMServerConfig::class.java
        }

        override fun applyEdited(oldItem: DetailedAEMServerConfig, newItem: DetailedAEMServerConfig) {
            oldItem.also {
                it.url = newItem.url
                it.name = newItem.name
                it.username = newItem.username
                it.password = newItem.password
            }
        }

        override fun edit(
            item: DetailedAEMServerConfig,
            mutator: Function<in DetailedAEMServerConfig, out DetailedAEMServerConfig>,
            isAdd: Boolean
        ) {
            val new = item.copy()
            if (AEMServerConfigurationEditDialog(new).showAndGet() && item != new) {
                mutator.`fun`(item).also {
                    it.id = new.id
                    it.url = new.url
                    it.name = new.name
                    it.username = new.username
                    it.password = new.password
                }
                tableModelEditor.model.fireTableDataChanged()
            }
        }

        override fun clone(item: DetailedAEMServerConfig, forInPlaceEditing: Boolean): DetailedAEMServerConfig {
            return item.copy(id = UUID.randomUUID().toString())
        }

        override fun isEmpty(item: DetailedAEMServerConfig): Boolean = item.name == ""

        override fun isUseDialogToAdd(): Boolean = true

        override fun isEditable(item: DetailedAEMServerConfig): Boolean = true
    }
}
