package com.kdiachenko.aem.filevault.settings.ui

import com.intellij.util.Function
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.table.TableModelEditor
import com.kdiachenko.aem.filevault.model.AEMServerConfig
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import com.kdiachenko.aem.filevault.model.toLazyDetailed
import com.kdiachenko.aem.filevault.settings.AEMServerSettings
import com.kdiachenko.aem.filevault.settings.SettingsPanel
import com.kdiachenko.aem.filevault.settings.service.CredentialsManager
import java.util.*
import javax.swing.DefaultCellEditor
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellEditor

class AEMServerSettingsPanel() : SettingsPanel {
    private lateinit var tableModelEditor: TableModelEditor<DetailedAEMServerConfig>
    private lateinit var initialDetailedConfigs: List<DetailedAEMServerConfig>

    override fun name(): String = "AEM VLT Settings"

    override fun getPanel(): JPanel {
        initialDetailedConfigs = state().configuredServers.toLazyDetailed()
        tableModelEditor = TableModelEditor(
            initialDetailedConfigs,
            createColumnInfos(),
            AEMServerEditor(),
            "Configure AEM servers"
        )

        tableModelEditor.modelListener(createDataChangedListener(tableModelEditor.model))
        tableModelEditor.enabled(true)
        tableModelEditor.setShowGrid(false)
        return tableModelEditor.createComponent() as JPanel
    }

    private fun createDataChangedListener(tableModel: ListTableModel<DetailedAEMServerConfig>):
            TableModelEditor.DataChangedListener<DetailedAEMServerConfig> =
        object : TableModelEditor.DataChangedListener<DetailedAEMServerConfig>() {
            private val model: ListTableModel<DetailedAEMServerConfig> = tableModel

            override fun dataChanged(
                columnInfo: ColumnInfo<DetailedAEMServerConfig?, *>,
                rowIndex: Int
            ) {
                if (columnInfo.name != "Default") {
                    return
                }
                val config = model.getItem(rowIndex)
                if (!config.isDefault) return
                for (i in 0 until model.rowCount) {
                    if (i == rowIndex) continue
                    val otherConfig = model.getItem(i)
                    if (!otherConfig.isDefault) continue
                    otherConfig.isDefault = false
                    model.fireTableCellUpdated(i, 0)
                }
            }

        }

    override fun isModified(): Boolean {
        return tableModelEditor.isModified
    }

    override fun save() {
        val newAEMServers = tableModelEditor.apply()

        val credentialsManager = CredentialsManager.getInstance()
        val newState = state()
        newState.configuredServers.forEach { credentialsManager.remove(it.id) }
        newState.clearConfiguredServers()
        newAEMServers.forEach {
            credentialsManager.add(it.id, it.username, it.password)
            newState.addServer(AEMServerConfig(it.id, it.name, it.url))
        }

        AEMServerSettings.Companion.getInstance().loadState(newState)

        initialDetailedConfigs = newAEMServers.toMutableCopy()
    }

    override fun reset() {
        tableModelEditor.reset(initialDetailedConfigs.toMutableCopy())
    }

    private fun List<DetailedAEMServerConfig>.toMutableCopy() =
        this.map { it.copy() }.toMutableList()

    private fun state() = AEMServerSettings.state()

    private fun createColumnInfos()
            : Array<ColumnInfo<DetailedAEMServerConfig, *>> {
        return arrayOf(
            createCheckboxColumnInfo("Default"),
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

    private fun createCheckboxColumnInfo(
        name: String
    ): TableModelEditor.EditableColumnInfo<DetailedAEMServerConfig, Boolean> {
        return object : TableModelEditor.EditableColumnInfo<DetailedAEMServerConfig, Boolean>(name) {
            override fun valueOf(config: DetailedAEMServerConfig): Boolean = config.isDefault
            override fun getEditor(item: DetailedAEMServerConfig?): TableCellEditor? = DefaultCellEditor(JCheckBox())
            override fun getColumnClass(): Class<Boolean> = Boolean::class.java

            override fun setValue(
                item: DetailedAEMServerConfig?,
                value: Boolean?
            ) = item?.isDefault = value ?: false

            override fun getWidth(table: JTable?): Int =
                table?.getFontMetrics(table.getFont())?.stringWidth(name)?.plus(15)
                    ?: super.getWidth(table)
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
                it.isDefault = newItem.isDefault
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
                    it.isDefault = new.isDefault
                }

                // If the edited item is now default, update other items
                if (new.isDefault) {
                    for (i in 0 until tableModelEditor.model.rowCount) {
                        val config = tableModelEditor.model.getItem(i)
                        if (config.id != new.id && config.isDefault) {
                            config.isDefault = false
                        }
                    }
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
