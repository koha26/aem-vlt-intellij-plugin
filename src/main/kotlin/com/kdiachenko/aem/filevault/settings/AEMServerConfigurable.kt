package com.kdiachenko.aem.filevault.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.kdiachenko.aem.filevault.settings.ui.AEMServerSettingsPanel
import javax.swing.*

/**
 * Settings UI for configuring AEM servers
 */
class AEMServerConfigurable() : Configurable {
    private var settingsPanel: SettingsPanel? = null

    override fun getDisplayName(): String = settingsPanel?.name() ?: "AEM FileVault Settings"

    override fun getPreferredFocusedComponent(): JComponent? {
        return settingsPanel?.getPanel()
    }

    override fun createComponent(): JComponent {
        settingsPanel = AEMServerSettingsPanel()
        return panel {
            group("AEM Servers") {
                row {
                    cell(settingsPanel!!.getPanel())
                        .align(Align.FILL)
                        .resizableColumn()
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return settingsPanel?.isModified() ?: false
    }

    @Throws(ConfigurationException::class)
    override fun apply() {
        if (settingsPanel?.isModified() == true) settingsPanel?.save()
    }

    override fun reset() {
        settingsPanel?.reset()
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }
}
