package com.github.derteufelqwe.intellijjavaindockerplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil


@State(
    name = "com.github.derteufelqwe.intellijjavaindockerplugin.settings.JDSettingsState",
    storages = [Storage("JavaDockerSettings.xml")]
)
class JDSettingsState : PersistentStateComponent<JDSettingsState> {

    var host = "localhost:2375"
    var socket = "/var/run/docker.sock"
    var protocol = Protocol.UNIX

    companion object {
        @JvmStatic
        fun getInstance() : JDSettingsState {
            return ApplicationManager.getApplication().getService(JDSettingsState::class.java)
        }
    }

    override fun getState(): JDSettingsState {
        return this
    }

    override fun loadState(state: JDSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}