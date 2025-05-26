package com.kdiachenko.aem.filevault.settings.ui

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.util.ui.UIUtil
import com.kdiachenko.aem.filevault.model.DetailedAEMServerConfig
import io.ktor.http.*
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.jetbrains.concurrency.AsyncPromise
import java.awt.event.ActionEvent
import javax.swing.*

class AEMServerConfigurationEditDialog(
    private val serverConfig: DetailedAEMServerConfig
) : DialogWrapper(null, false) {

    private val urlRegex = Regex(
        "https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}(\\.[a-zA-Z0-9()]{1,6})?\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)"
    )

    private lateinit var urlField: JBTextField
    private lateinit var userField: JBTextField
    private lateinit var passwordField: JPasswordField
    private lateinit var defaultCheckbox: JCheckBox
    private lateinit var testResultField: JLabel
    private lateinit var testServerAction: Action

    init {
        title = "AEM Server Configuration"
        init()
    }

    @Suppress("kotlin:S3776")
    override fun createCenterPanel(): JComponent = panel {
        row("Server Name: ") {
            textField()
                .align(Align.Companion.FILL)
                .resizableColumn()
                .bindText({ serverConfig.name }, { serverConfig.name = it })
                .validationOnInput { validateNotEmpty(it.text) }
                .validationOnApply { validateNotEmpty(it.text) }
                .focused()
        }
        row("URL: ") {
            urlField = textField()
                .align(Align.Companion.FILL)
                .resizableColumn()
                .bindText({ serverConfig.url }, { serverConfig.url = it })
                .validationOnInput { validateUrl(it.text) }
                .validationOnApply { validateUrl(it.text) }
                .component
        }

        row("Username: ") {
            userField = textField()
                .align(Align.Companion.FILL)
                .resizableColumn()
                .bindText(
                    { serverConfig.username },
                    { serverConfig.username = it }
                )
                .component
        }

        row("Password: ") {
            passwordField = passwordField()
                .align(Align.Companion.FILL)
                .columns(COLUMNS_SHORT)
                .resizableColumn()
                .bind(
                    JPasswordField::getPassword,
                    { field, value -> field.text = String(value) },
                    MutableProperty(
                        { serverConfig.password.toCharArray() },
                        { serverConfig.password = String(it) }
                    )
                )
                .component
        }
        row {
            defaultCheckbox = checkBox("Is default?")
                .comment(
                    """
                    Check if this is default AEM server configuration. 
                    Only one configuration can be default. 
                    Another default configuration will be unchecked. 
                    Click 'Save' button to save changes.
                """.trimIndent(), maxLineLength = 35
                )
                .bind(
                    JCheckBox::isSelected,
                    { field, value -> field.isSelected = value },
                    MutableProperty(
                        { serverConfig.isDefault },
                        { serverConfig.isDefault = it }
                    )
                )
                .component
        }

        row {
            testResultField = label("")
                .align(Align.Companion.FILL)
                .resizableColumn()
                .component
                .also {
                    it.isFocusable = false
                    it.isVisible = false
                }
        }.layout(RowLayout.INDEPENDENT).resizableRow()
    }

    override fun createDefaultActions() {
        super.createDefaultActions()

        testServerAction = object : DialogWrapperAction("Test Connection") {
            override fun doAction(e: ActionEvent) {
                okAction.disable()
                testConnectionToServer()
            }
        }

        okAction.addPropertyChangeListener {
            if ("enabled" == it.propertyName) {
                testServerAction.isEnabled = it.newValue as Boolean
            }
        }
    }

    override fun createActions(): Array<out Action> {
        return arrayOf(testServerAction, *super.createActions())
    }


    private fun testConnectionToServer() {
        val validationInfos = doValidateAll()

        updateErrorInfo(validationInfos)

        if (validationInfos.isEmpty()) {
            SwingUtilities.invokeLater {
                updateTestResult("")
            }

            val promise = AsyncPromise<Int>().also {
                it.onSuccess { status ->
                    runInEdt {
                        if (status == 200) {
                            updateTestResult("Connection successful.")
                        } else {
                            updateTestResult("Connection failed. Response status: $status", true)
                        }
                        okAction.enable()
                        testServerAction.enable()
                    }
                }.onError { ex ->
                    runInEdt {
                        val rootCause = ExceptionUtils.getRootCause(ex)
                        val message = rootCause.localizedMessage ?: rootCause.javaClass.simpleName

                        updateTestResult("Connection failed. Error: $message", true)
                        okAction.enable()
                        testServerAction.enable()
                    }
                }
            }

            runBackgroundableTask("Checking AEM Server Connection", null, true) {
                try {
                    val status =
                        testConnectionToAEMServer(urlField.text, userField.text, String(passwordField.password))
                    promise.setResult(status)
                } catch (th: Throwable) {
                    thisLogger().info(th)
                    promise.setError(th)
                }
            }
        } else {
            val firstError = validationInfos.first()

            if (firstError.component?.isVisible == true) {
                IdeFocusManager.getInstance(null).requestFocus(firstError.component!!, true)
            }
        }
    }

    private fun updateTestResult(text: String, error: Boolean = false) {
        val preferredSize = testResultField.preferredSize
        val color = if (error) UIUtil.getErrorForeground() else UIUtil.getLabelSuccessForeground()
        val html = HtmlBuilder()
            .append(HtmlChunk.raw(text).wrapWith(HtmlChunk.font(ColorUtil.toHex(color))))
            .wrapWithHtmlBody().toString()

        testResultField.text = html

        testResultField.preferredSize = preferredSize
        testResultField.isVisible = text.isNotBlank()
    }

    private fun ValidationInfoBuilder.validateNotEmpty(value: String): ValidationInfo? {
        if (value.isEmpty()) {
            return error("Must not be empty")
        }
        return null
    }

    private fun ValidationInfoBuilder.validateUrl(value: String): ValidationInfo? {
        val validationInfo = validateNotEmpty(value)

        if (validationInfo != null) {
            return validationInfo
        }

        if (!urlRegex.matches(value)) {
            return error("Not a valid url")
        }

        return null
    }

    private fun testConnectionToAEMServer(urlString: String, username: String, password: String): Int {
        val credentialsProvider: CredentialsProvider = BasicCredentialsProvider()
        val url = Url(urlString)
        credentialsProvider.setCredentials(
            AuthScope(url.host, AuthScope.ANY_PORT),
            UsernamePasswordCredentials(username, password)
        )

        val httpClient = HttpClients.custom()
            .setDefaultCredentialsProvider(credentialsProvider)
            .build()
        return httpClient.use {
            val request = HttpGet(urlString)
            val response: CloseableHttpResponse = httpClient.execute(request)

            return@use response.statusLine.statusCode
        }
    }

    fun Action.enable() {
        this.isEnabled = true
    }

    fun Action.disable() {
        this.isEnabled = false
    }
}
