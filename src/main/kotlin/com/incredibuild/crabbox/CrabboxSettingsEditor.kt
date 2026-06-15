package com.incredibuild.crabbox

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class CrabboxSettingsEditor : SettingsEditor<CrabboxRunConfiguration>() {
    private val executableField = JBTextField()
    private val workingDirectoryField = JBTextField()
    private val crabboxArgsField = RawCommandLineEditor()
    private val rustCommandField = RawCommandLineEditor()
    private val envTextArea = JBTextArea(6, 60)

    override fun resetEditorFrom(configuration: CrabboxRunConfiguration) {
        executableField.text = configuration.crabboxExecutable
        workingDirectoryField.text = configuration.workingDirectory
        crabboxArgsField.text = configuration.crabboxArgs
        rustCommandField.text = configuration.rustCommand
        envTextArea.text = configuration.envText
    }

    override fun applyEditorTo(configuration: CrabboxRunConfiguration) {
        try {
            CrabboxCommandLine.splitArgs(crabboxArgsField.text)
            CrabboxCommandLine.splitArgs(rustCommandField.text)
            configuration.parseEnv(envTextArea.text)
        } catch (error: IllegalArgumentException) {
            throw ConfigurationException(error.message ?: "Invalid Crabbox run configuration")
        }

        configuration.crabboxExecutable = executableField.text.ifBlank { "crabbox" }
        configuration.workingDirectory = workingDirectoryField.text
        configuration.crabboxArgs = crabboxArgsField.text
        configuration.rustCommand = rustCommandField.text
        configuration.envText = envTextArea.text
    }

    override fun createEditor(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("Crabbox executable:", executableField)
            .addLabeledComponent("Working directory:", workingDirectoryField)
            .addLabeledComponent("Crabbox args:", crabboxArgsField)
            .addLabeledComponent("Rust/Cargo command:", rustCommandField)
            .addLabeledComponent("Environment (KEY=value, one per line):", envTextArea)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }
}
