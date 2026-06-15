package com.incredibuild.crabbox

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil

@Service(Service.Level.APP)
@State(name = "CrabboxSettings", storages = [Storage("crabbox.xml")])
class CrabboxSettingsState : PersistentStateComponent<CrabboxSettingsState.State> {
    class State {
        var crabboxExecutable: String = "crabbox"
        var brokerUrl: String = ""
        var defaultProvider: String = ""
        var defaultClass: String = ""
        var defaultCrabboxArgs: String = ""
        var isloImage: String = DEFAULT_ISLO_RUST_IMAGE
    }

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    fun defaultRunArgs(): List<String> {
        val args = mutableListOf<String>()
        if (state.defaultProvider.isNotBlank()) {
            args += "--provider"
            args += state.defaultProvider.trim()
        }
        if (state.defaultClass.isNotBlank()) {
            args += "--class"
            args += state.defaultClass.trim()
        }
        args += CrabboxCommandLine.splitArgs(state.defaultCrabboxArgs)
        if (state.defaultProvider.equals("islo", ignoreCase = true)) {
            return withIsloRustImage(args)
        }
        return args
    }

    fun isloRunArgs(): List<String> {
        return withIsloRustImage(withProvider(defaultRunArgs(), "islo"))
    }

    private fun withProvider(args: List<String>, provider: String): List<String> {
        return withoutFlagValue(args, "--provider") + listOf("--provider", provider)
    }

    private fun withIsloRustImage(args: List<String>): List<String> {
        if (hasFlag(args, "--islo-image")) {
            return args
        }

        val image = state.isloImage.ifBlank { DEFAULT_ISLO_RUST_IMAGE }.trim()
        return args + listOf("--islo-image", image)
    }

    private fun hasFlag(args: List<String>, flag: String): Boolean {
        return args.any { it == flag || it.startsWith("$flag=") }
    }

    private fun withoutFlagValue(args: List<String>, flag: String): List<String> {
        val normalized = mutableListOf<String>()
        var skipNext = false
        for (arg in args) {
            when {
                skipNext -> skipNext = false
                arg == flag -> skipNext = true
                arg.startsWith("$flag=") -> Unit
                else -> normalized += arg
            }
        }
        return normalized
    }

    companion object {
        const val DEFAULT_ISLO_RUST_IMAGE =
            "ghcr.io/zozo123/rust-rover-plugin-crabbox/crabbox-rust-runner:0.3.2"

        fun getInstance(): CrabboxSettingsState = service()
    }
}
