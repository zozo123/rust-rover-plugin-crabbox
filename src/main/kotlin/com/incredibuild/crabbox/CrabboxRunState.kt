package com.incredibuild.crabbox

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView

class CrabboxRunState(
    private val executionEnvironment: ExecutionEnvironment,
    private val configuration: CrabboxRunConfiguration,
) : CommandLineState(executionEnvironment) {
    override fun startProcess(): ProcessHandler {
        val commandLine = try {
            when (configuration.executionMode) {
                CrabboxExecutionMode.RUN -> CrabboxCommandLine.run(
                    project = executionEnvironment.project,
                    crabboxExecutable = configuration.crabboxExecutable,
                    workingDirectory = configuration.workingDirectory,
                    crabboxArgs = configuration.crabboxArgs,
                    rustCommand = configuration.rustCommand,
                    env = CrabboxSecrets.withConfiguredSecrets(configuration.env),
                    // Forward only the user-declared env vars to the remote command.
                    // ISLO_API_KEY is consumed locally by the crabbox CLI for auth and
                    // must not be whitelisted into the sandbox.
                    allowEnv = configuration.env.keys,
                )

                CrabboxExecutionMode.SIMPLE -> CrabboxCommandLine.simple(
                    project = executionEnvironment.project,
                    crabboxExecutable = configuration.crabboxExecutable,
                    workingDirectory = configuration.workingDirectory,
                    args = CrabboxCommandLine.splitArgs(configuration.simpleArgs),
                    env = CrabboxSecrets.withConfiguredSecrets(configuration.env),
                )
            }
        } catch (error: IllegalArgumentException) {
            throw ExecutionException(error.message ?: "Invalid Crabbox command", error)
        }

        return try {
            OSProcessHandler(commandLine)
        } catch (error: Exception) {
            throw ExecutionException("Failed to start Crabbox: ${error.message}", error)
        }
    }

    override fun createConsole(executor: Executor): ConsoleView {
        val builder = TextConsoleBuilderFactory.getInstance().createBuilder(executionEnvironment.project)
        builder.addFilter(CrabboxConsoleFilter(executionEnvironment.project))
        return builder.console
    }
}
