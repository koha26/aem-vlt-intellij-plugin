package com.kdiachenko.aem.filevault.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.kdiachenko.aem.filevault.model.AEMServer
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

/**
 * Settings UI for configuring AEM servers
 */
class AEMServerConfigurable : Configurable {
    private val settings = AEMServerSettings.getInstance()
    private val serversListModel = DefaultListModel<AEMServer>()
    private val serversList = JBList(serversListModel)

    private val nameField = JBTextField()
    private val urlField = JBTextField()
    private val usernameField = JBTextField()
    private val passwordField = JPasswordField()
    private val isDefaultCheckbox = JCheckBox("Set as default")

    private var selectedServer: AEMServer? = null
    private var modified = false

    private val detailPanel = createDetailPanel()
    private val mainPanel = createMainPanel()

    override fun getDisplayName(): String = "AEM FileVault Settings"

    override fun createComponent(): JComponent = mainPanel

    override fun isModified(): Boolean = modified

    override fun apply() {
        // Save current settings
        applyServerDetails()
        settings.servers.clear()
        for (i in 0 until serversListModel.size()) {
            settings.servers.add(serversListModel.getElementAt(i))
        }
        modified = false
    }

    override fun reset() {
        // Load settings
        serversListModel.clear()
        settings.servers.forEach { serversListModel.addElement(it) }
        updateDetailPanel(null)
        modified = false
    }

    private fun createMainPanel(): JPanel {
        // Initialize list with servers
        settings.servers.forEach { serversListModel.addElement(it) }

        serversList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        serversList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                applyServerDetails()
                updateDetailPanel(serversList.selectedValue)
            }
        }

        val decoratedList = ToolbarDecorator.createDecorator(serversList)
            .setAddAction { addServer() }
            .setRemoveAction { removeServer() }
            .disableUpDownActions()
            .createPanel()

        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
        splitPane.leftComponent = decoratedList
        splitPane.rightComponent = detailPanel
        splitPane.dividerLocation = 200

        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(splitPane, BorderLayout.CENTER)

        return panel
    }

    private fun createDetailPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(GridBagLayout())
        val constraints = GridBagConstraints()

        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.weightx = 0.0
        constraints.gridx = 0
        constraints.gridy = 0
        constraints.insets = Insets(5, 5, 5, 5)
        panel.add(JLabel("Name:"), constraints)

        constraints.gridy = 1
        panel.add(JLabel("URL:"), constraints)

        constraints.gridy = 2
        panel.add(JLabel("Username:"), constraints)

        constraints.gridy = 3
        panel.add(JLabel("Password:"), constraints)

        constraints.weightx = 1.0
        constraints.gridx = 1
        constraints.gridy = 0
        panel.add(nameField, constraints)

        constraints.gridy = 1
        panel.add(urlField, constraints)

        constraints.gridy = 2
        panel.add(usernameField, constraints)

        constraints.gridy = 3
        panel.add(passwordField, constraints)

        constraints.gridy = 4
        constraints.gridx = 0
        constraints.gridwidth = 2
        panel.add(isDefaultCheckbox, constraints)

        // Add listeners
        nameField.document.addDocumentListener(SimpleDocumentListener { modified = true })
        urlField.document.addDocumentListener(SimpleDocumentListener { modified = true })
        usernameField.document.addDocumentListener(SimpleDocumentListener { modified = true })
        passwordField.document.addDocumentListener(SimpleDocumentListener { modified = true })
        isDefaultCheckbox.addActionListener { modified = true }

        // Disable all fields initially
        setFieldsEnabled(false)

        return panel
    }

    private fun updateDetailPanel(server: AEMServer?) {
        selectedServer = server
        if (server == null) {
            nameField.text = ""
            urlField.text = ""
            usernameField.text = ""
            passwordField.text = ""
            isDefaultCheckbox.isSelected = false
            setFieldsEnabled(false)
        } else {
            nameField.text = server.name
            urlField.text = server.url
            usernameField.text = server.username
            passwordField.text = server.password
            isDefaultCheckbox.isSelected = server.isDefault
            setFieldsEnabled(true)
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        nameField.isEnabled = enabled
        urlField.isEnabled = enabled
        usernameField.isEnabled = enabled
        passwordField.isEnabled = enabled
        isDefaultCheckbox.isEnabled = enabled
    }

    private fun addServer() {
        val newServer = AEMServer(
            name = "New Server",
            url = "http://localhost:4502",
            username = "admin",
            password = "admin",
            isDefault = serversListModel.isEmpty
        )
        serversListModel.addElement(newServer)
        serversList.selectedIndex = serversListModel.size() - 1
        modified = true
    }

    private fun removeServer() {
        val selectedIndex = serversList.selectedIndex
        if (selectedIndex >= 0) {
            serversListModel.remove(selectedIndex)
            if (serversListModel.size() > 0) {
                serversList.selectedIndex = Math.min(selectedIndex, serversListModel.size() - 1)
            } else {
                updateDetailPanel(null)
            }
            modified = true
        }
    }

    private fun applyServerDetails() {
        val server = selectedServer ?: return

        val newServer = AEMServer(
            name = nameField.text,
            url = urlField.text,
            username = usernameField.text,
            password = String(passwordField.password),
            isDefault = isDefaultCheckbox.isSelected
        )

        // If this is the default and there are others, unset them
        if (newServer.isDefault) {
            for (i in 0 until serversListModel.size()) {
                val current = serversListModel.getElementAt(i)
                if (current != server && current.isDefault) {
                    current.isDefault = false
                }
            }
        }

        val index = serversList.selectedIndex
        if (index >= 0) {
            serversListModel.set(index, newServer)
            selectedServer = newServer
        }
    }
}

/**
 * Simple document listener implementation
 */
class SimpleDocumentListener(private val onChange: () -> Unit) : javax.swing.event.DocumentListener {
    override fun insertUpdate(e: javax.swing.event.DocumentEvent) = onChange()
    override fun removeUpdate(e: javax.swing.event.DocumentEvent) = onChange()
    override fun changedUpdate(e: javax.swing.event.DocumentEvent) = onChange()
}
