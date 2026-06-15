package com.incredibuild.crabbox

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JPasswordField

class CrabboxSettingsConfigurable : Configurable {
    private val executableField = JBTextField()
    private val brokerUrlField = JBTextField()
    private val defaultProviderField = JBTextField()
    private val defaultClassField = JBTextField()
    private val defaultCrabboxArgsField = JBTextField()
    private val isloImageField = JBTextField()
    private val isloApiKeyField = JPasswordField()
    private var panel: JPanel? = null
    private var originalIsloApiKey = ""

    override fun getDisplayName(): String = "Crabbox"

    override fun createComponent(): JComponent {
        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Crabbox executable:", executableField)
            .addLabeledComponent("Broker URL:", brokerUrlField)
            .addLabeledComponent("Default provider:", defaultProviderField)
            .addLabeledComponent("Default class:", defaultClassField)
            .addLabeledComponent("Default Crabbox args:", defaultCrabboxArgsField)
            .addSeparator()
            .addLabeledComponent("Islo Rust image:", isloImageField)
            .addLabeledComponent("Islo API key:", isloApiKeyField)
            .addComponentFillVertically(JPanel(), 0)
            .panel
        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val state = CrabboxSettingsState.getInstance().state
        return executableField.text != state.crabboxExecutable ||
            brokerUrlField.text != state.brokerUrl ||
            defaultProviderField.text != state.defaultProvider ||
            defaultClassField.text != state.defaultClass ||
            defaultCrabboxArgsField.text != state.defaultCrabboxArgs ||
            isloImageField.text != state.isloImage.ifBlank { CrabboxSettingsState.DEFAULT_ISLO_RUST_IMAGE } ||
            String(isloApiKeyField.password) != originalIsloApiKey
    }

    override fun apply() {
        try {
            CrabboxCommandLine.splitArgs(defaultCrabboxArgsField.text)
        } catch (error: IllegalArgumentException) {
            throw ConfigurationException(error.message ?: "Invalid Crabbox args")
        }

        val state = CrabboxSettingsState.getInstance().state
        state.crabboxExecutable = executableField.text.ifBlank { "crabbox" }
        state.brokerUrl = brokerUrlField.text.trim()
        state.defaultProvider = defaultProviderField.text.trim()
        state.defaultClass = defaultClassField.text.trim()
        state.defaultCrabboxArgs = defaultCrabboxArgsField.text.trim()
        state.isloImage = isloImageField.text.ifBlank {
            CrabboxSettingsState.DEFAULT_ISLO_RUST_IMAGE
        }.trim()
        CrabboxSecrets.setIsloApiKey(String(isloApiKeyField.password))
        originalIsloApiKey = CrabboxSecrets.getIsloApiKey()
    }

    override fun reset() {
        val state = CrabboxSettingsState.getInstance().state
        executableField.text = state.crabboxExecutable
        brokerUrlField.text = state.brokerUrl
        defaultProviderField.text = state.defaultProvider
        defaultClassField.text = state.defaultClass
        defaultCrabboxArgsField.text = state.defaultCrabboxArgs
        isloImageField.text = state.isloImage.ifBlank {
            CrabboxSettingsState.DEFAULT_ISLO_RUST_IMAGE
        }
        originalIsloApiKey = CrabboxSecrets.getIsloApiKey()
        isloApiKeyField.text = originalIsloApiKey
    }
}
