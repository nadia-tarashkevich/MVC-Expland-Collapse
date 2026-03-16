package com.github.nadiatarashkevich.mvcexplandcollapse.settings

import com.intellij.openapi.components.*

@Service(Service.Level.APP)
@State(
    name = "com.github.nadiatarashkevich.mvcexplandcollapse.settings.MvcSettingsState",
    storages = [Storage("MvcExpandCollapseSettings.xml")]
)
class MvcSettingsState : PersistentStateComponent<MvcSettingsState.State> {
    data class State(
        var folderNames: List<String> = listOf("app", "controllers", "views"),
        var firstLaunch: Boolean = true
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        val instance: MvcSettingsState
            get() = service<MvcSettingsState>()
    }
}
