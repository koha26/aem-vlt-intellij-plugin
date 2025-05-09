package com.kdiachenko.aem.filevault.settings

import javax.swing.JPanel

interface SettingsPanel {

  fun name(): String

  fun getPanel(): JPanel

  fun isModified(): Boolean

  fun save()

  fun reset()

}
