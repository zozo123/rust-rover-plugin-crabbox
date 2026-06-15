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
        return args
    }

    companion object {
        fun getInstance(): CrabboxSettingsState = service()
    }
}
