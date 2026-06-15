package com.incredibuild.crabbox

import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.project.Project

object CrabboxTaskRunner {
    fun runRust(
        project: Project,
        title: String,
        rustCommand: String,
        crabboxArgs: List<String> = CrabboxSettingsState.getInstance().defaultRunArgs(),
    ) {
        val settings = CrabboxSettingsState.getInstance()
        val runnerSettings = createTemporaryConfiguration(project, title)
        val configuration = runnerSettings.configuration as CrabboxRunConfiguration
        configuration.executionMode = CrabboxExecutionMode.RUN
        configuration.crabboxExecutable = settings.state.crabboxExecutable
        configuration.workingDirectory = project.basePath.orEmpty()
        configuration.crabboxArgs = CrabboxCommandLine.joinArgs(crabboxArgs)
        configuration.rustCommand = rustCommand
        execute(project, runnerSettings)
    }

    fun runSimple(
        project: Project,
        title: String,
        args: List<String>,
    ) {
        val settings = CrabboxSettingsState.getInstance()
        val runnerSettings = createTemporaryConfiguration(project, title)
        val configuration = runnerSettings.configuration as CrabboxRunConfiguration
        configuration.executionMode = CrabboxExecutionMode.SIMPLE
        configuration.crabboxExecutable = settings.state.crabboxExecutable
        configuration.workingDirectory = project.basePath.orEmpty()
        configuration.simpleArgs = CrabboxCommandLine.joinArgs(args)
        execute(project, runnerSettings)
    }

    private fun createTemporaryConfiguration(
        project: Project,
        title: String,
    ): RunnerAndConfigurationSettings {
        val type = ConfigurationTypeUtil.findConfigurationType(CrabboxRunConfigurationType::class.java)
        val factory = type.configurationFactories.first()
        val settings = RunManager.getInstance(project).createConfiguration(title, factory)
        settings.isTemporary = true
        return settings
    }

    private fun execute(project: Project, settings: RunnerAndConfigurationSettings) {
        val runManager = RunManager.getInstance(project)
        settings.isTemporary = true
        runManager.setTemporaryConfiguration(settings)
        runManager.selectedConfiguration = settings
        ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
    }
}
