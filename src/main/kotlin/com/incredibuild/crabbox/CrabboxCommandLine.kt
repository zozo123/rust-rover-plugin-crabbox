package com.incredibuild.crabbox

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import java.io.File

object CrabboxCommandLine {
    fun run(
        project: Project,
        crabboxExecutable: String,
        workingDirectory: String,
        crabboxArgs: String,
        rustCommand: String,
        env: Map<String, String> = emptyMap(),
        allowEnv: Collection<String> = emptyList(),
    ): GeneralCommandLine {
        return run(
            project = project,
            crabboxExecutable = crabboxExecutable,
            workingDirectory = workingDirectory,
            crabboxArgs = splitArgs(crabboxArgs),
            rustCommand = splitArgs(rustCommand),
            env = env,
            allowEnv = allowEnv,
        )
    }

    fun run(
        project: Project,
        crabboxExecutable: String,
        workingDirectory: String,
        crabboxArgs: List<String>,
        rustCommand: List<String>,
        env: Map<String, String> = emptyMap(),
        allowEnv: Collection<String> = emptyList(),
    ): GeneralCommandLine {
        require(rustCommand.isNotEmpty()) { "Rust/Cargo command is required" }
        require("--" !in crabboxArgs) {
            "Do not include -- in Crabbox args; the plugin inserts the command separator."
        }

        return base(project, crabboxExecutable, workingDirectory, env).apply {
            addParameter("run")
            crabboxArgs.forEach(::addParameter)
            allowEnvArgs(crabboxArgs, allowEnv).forEach(::addParameter)
            addParameter("--")
            rustCommand.forEach(::addParameter)
        }
    }

    /**
     * Crabbox forwards a deliberately narrow environment to the remote command
     * (only `NODE_OPTIONS` and `CI`); every other variable must be whitelisted
     * with `--allow-env NAME`. Run configurations carry user-defined env vars
     * that are otherwise silently dropped before they reach Cargo, so emit an
     * `--allow-env` flag for each one the caller wants forwarded, skipping any
     * already declared in [crabboxArgs] and de-duplicating the rest.
     */
    fun allowEnvArgs(crabboxArgs: List<String>, allowEnv: Collection<String>): List<String> {
        val alreadyAllowed = existingAllowEnv(crabboxArgs)
        val result = mutableListOf<String>()
        val seen = HashSet(alreadyAllowed)
        for (name in allowEnv) {
            val trimmed = name.trim()
            if (trimmed.isEmpty() || !seen.add(trimmed)) continue
            result += "--allow-env"
            result += trimmed
        }
        return result
    }

    private fun existingAllowEnv(crabboxArgs: List<String>): Set<String> {
        val names = mutableSetOf<String>()
        var captureNext = false
        for (arg in crabboxArgs) {
            when {
                captureNext -> {
                    arg.split(',').forEach { names += it.trim() }
                    captureNext = false
                }
                arg == "--allow-env" || arg == "-allow-env" -> captureNext = true
                arg.startsWith("--allow-env=") -> arg.removePrefix("--allow-env=").split(',').forEach { names += it.trim() }
                arg.startsWith("-allow-env=") -> arg.removePrefix("-allow-env=").split(',').forEach { names += it.trim() }
            }
        }
        names.removeAll { it.isEmpty() }
        return names
    }

    fun simple(
        project: Project,
        crabboxExecutable: String,
        workingDirectory: String,
        args: List<String>,
        env: Map<String, String> = emptyMap(),
    ): GeneralCommandLine {
        require(args.isNotEmpty()) { "Crabbox command is required" }

        return base(project, crabboxExecutable, workingDirectory, env).apply {
            args.forEach(::addParameter)
        }
    }

    fun splitArgs(value: String): List<String> {
        if (value.isBlank()) return emptyList()

        val args = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaping = false
        var sawToken = false

        fun flush() {
            if (sawToken) {
                args += current.toString()
                current.setLength(0)
                sawToken = false
            }
        }

        for (char in value) {
            when {
                escaping -> {
                    current.append(char)
                    escaping = false
                    sawToken = true
                }

                char == '\\' && quote != '\'' -> {
                    escaping = true
                    sawToken = true
                }

                quote != null -> {
                    if (char == quote) {
                        quote = null
                    } else {
                        current.append(char)
                    }
                    sawToken = true
                }

                char == '\'' || char == '"' -> {
                    quote = char
                    sawToken = true
                }

                char.isWhitespace() -> flush()

                else -> {
                    current.append(char)
                    sawToken = true
                }
            }
        }

        require(!escaping) { "Trailing escape in command line" }
        require(quote == null) { "Unclosed quoted string in command line" }
        flush()
        return args
    }

    fun joinArgs(args: List<String>): String {
        return args.joinToString(" ") { arg ->
            if (arg.isNotEmpty() && arg.none { it.isWhitespace() || it == '"' || it == '\\' || it == '\'' }) {
                arg
            } else {
                "\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
            }
        }
    }

    private fun base(
        project: Project,
        crabboxExecutable: String,
        workingDirectory: String,
        env: Map<String, String>,
    ): GeneralCommandLine {
        return GeneralCommandLine().apply {
            exePath = crabboxExecutable.ifBlank { "crabbox" }
            workDirectory = File(workingDirectory.ifBlank { project.basePath ?: "." })
            environment.putAll(env)
            withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
        }
    }
}
