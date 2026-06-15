package com.incredibuild.crabbox

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class CrabboxRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : RunConfigurationBase<CrabboxRunConfigurationOptions>(project, factory, name) {
    override fun getOptionsClass(): Class<out RunConfigurationOptions> {
        return CrabboxRunConfigurationOptions::class.java
    }

    override fun getOptions(): CrabboxRunConfigurationOptions {
        return super.getOptions() as CrabboxRunConfigurationOptions
    }

    var executionMode: CrabboxExecutionMode
        get() = CrabboxExecutionMode.valueOf(options.executionMode ?: CrabboxExecutionMode.RUN.name)
        set(value) {
            options.executionMode = value.name
        }

    var crabboxExecutable: String
        get() = options.crabboxExecutable ?: "crabbox"
        set(value) {
            options.crabboxExecutable = value
        }

    var workingDirectory: String
        get() = options.workingDirectory ?: project.basePath.orEmpty()
        set(value) {
            options.workingDirectory = value
        }

    var rustCommand: String
        get() = options.rustCommand ?: "cargo test"
        set(value) {
            options.rustCommand = value
        }

    var crabboxArgs: String
        get() = options.crabboxArgs ?: ""
        set(value) {
            options.crabboxArgs = value
        }

    var simpleArgs: String
        get() = options.simpleArgs ?: ""
        set(value) {
            options.simpleArgs = value
        }

    var envText: String
        get() = options.envText ?: ""
        set(value) {
            options.envText = value
        }

    val env: Map<String, String>
        get() = parseEnv(envText)

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return CrabboxSettingsEditor()
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return CrabboxRunState(environment, this)
    }

    override fun checkConfiguration() {
        if (crabboxExecutable.isBlank()) {
            throw RuntimeConfigurationException("Crabbox executable is required")
        }

        try {
            when (executionMode) {
                CrabboxExecutionMode.RUN -> {
                    if (rustCommand.isBlank()) {
                        throw RuntimeConfigurationException("Rust/Cargo command is required")
                    }
                    val parsedCrabboxArgs = CrabboxCommandLine.splitArgs(crabboxArgs)
                    CrabboxCommandLine.splitArgs(rustCommand)
                    if ("--" in parsedCrabboxArgs) {
                        throw RuntimeConfigurationException(
                            "Do not include -- in Crabbox args; the plugin inserts it.",
                        )
                    }
                }

                CrabboxExecutionMode.SIMPLE -> {
                    if (simpleArgs.isBlank()) {
                        throw RuntimeConfigurationException("Crabbox command is required")
                    }
                    CrabboxCommandLine.splitArgs(simpleArgs)
                }
            }
            parseEnv(envText)
        } catch (error: IllegalArgumentException) {
            throw RuntimeConfigurationException(error.message ?: "Invalid Crabbox configuration")
        }
    }

    fun parseEnv(value: String): Map<String, String> {
        return value
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map {
                val index = it.indexOf('=')
                require(index > 0) { "Environment entries must use KEY=value: $it" }
                it.substring(0, index) to it.substring(index + 1)
            }
            .toMap()
    }
}
