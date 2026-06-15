package com.incredibuild.crabbox

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project

class CrabboxRunConfigurationType : ConfigurationTypeBase(
    ID,
    "Crabbox",
    "Run commands in an OpenClaw Crabbox sandbox",
    AllIcons.RunConfigurations.Application,
) {
    init {
        addFactory(CrabboxRunConfigurationFactory(this))
    }

    companion object {
        const val ID = "CRABBOX_RUN_CONFIGURATION"
    }
}

class CrabboxRunConfigurationFactory(
    type: ConfigurationType,
) : ConfigurationFactory(type) {
    override fun createTemplateConfiguration(project: Project): CrabboxRunConfiguration {
        val settings = CrabboxSettingsState.getInstance()
        return CrabboxRunConfiguration(project, this, "Crabbox cargo test").apply {
            crabboxExecutable = settings.state.crabboxExecutable
            crabboxArgs = CrabboxCommandLine.joinArgs(settings.defaultRunArgs())
            rustCommand = "cargo test"
            workingDirectory = project.basePath.orEmpty()
        }
    }

    override fun getId(): String = "CrabboxRunConfigurationFactory"
}
