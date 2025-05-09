package com.kdiachenko.aem.filevault.settings.ui

import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.panel
import com.kdiachenko.aem.filevault.settings.AEMServerConfigurable
import com.kdiachenko.aem.filevault.settings.AEMServerSettings
import javax.swing.JComponent

class ServerConfigToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val toolWindowContent = ServerConfigToolWindowContent(project)
        val content = ContentFactory.getInstance().createContent(
            toolWindowContent.getContent(),
            "",
            false
        )
        toolWindow.contentManager.addContent(content)
    }
}

class ServerConfigToolWindowContent(private val project: Project) {
    private val settings = AEMServerSettings.Companion.getInstance()

    fun getContent(): JComponent {
        return panel {
            group("Server Configurations") {
                row {
                    label("Active Server:").bold()
                    val defaultConfig = settings.state.getDefaultServer()
                    if (defaultConfig != null) {
                        label(defaultConfig.name)
                    } else {
                        label("No default server configured")
                    }
                }

                indent {
                    row {
                        label("Server Details:").bold()
                    }

                    val defaultConfig = settings.state.getDefaultServer()
                    if (defaultConfig != null) {
                        row {
                            label("URL:").bold()
                            label(defaultConfig.url)
                        }

                        row {
                            label("Username:").bold()
                            //label(defaultConfig.username)
                        }
                    }
                }

                row {
                    button("Manage Configurations") {
                        ShowSettingsUtil.getInstance().showSettingsDialog(
                            project,
                            AEMServerConfigurable::class.java
                        )
                    }
                }
            }

            group("Actions") {
                row {
                    button("Connect to Server") {
                        // Implement connection logic here
                    }.enabled(settings.state.getDefaultServer() != null)
                }

                row {
                    button("Refresh Connection") {
                        // Implement refresh logic here
                    }.enabled(settings.state.getDefaultServer() != null)
                }
            }
        }
    }
}
