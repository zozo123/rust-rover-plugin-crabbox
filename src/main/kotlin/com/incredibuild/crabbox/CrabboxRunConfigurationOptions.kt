package com.incredibuild.crabbox

import com.intellij.execution.configurations.RunConfigurationOptions

class CrabboxRunConfigurationOptions : RunConfigurationOptions() {
    var executionMode by string(CrabboxExecutionMode.RUN.name)
    var crabboxExecutable by string("crabbox")
    var workingDirectory by string("")
    var rustCommand by string("cargo test")
    var crabboxArgs by string("")
    var simpleArgs by string("")
    var envText by string("")
}

enum class CrabboxExecutionMode {
    RUN,
    SIMPLE,
}
