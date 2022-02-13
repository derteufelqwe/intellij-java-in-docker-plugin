package com.github.derteufelqwe.intellijjavaindockerplugin.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

class SettingsConfigurable : Configurable {

    private var settingsComponent: SettingsComponent? = null

    override fun createComponent(): JComponent {
        settingsComponent = SettingsComponent()
        return settingsComponent!!.getPanel()
    }

    override fun isModified(): Boolean {
        val settings = JDSettingsState.getInstance()

        settingsComponent?.let {
            return (it.hostText != settings.host) or
                    (it.socketText != settings.socket) or
                    (it.protocol != settings.protocol)
        }

        return false
    }

    override fun apply() {
        val settings = JDSettingsState.getInstance()

        settingsComponent?.let {
            settings.host = it.hostText
            settings.socket = it.socketText
            settings.protocol = it.protocol
        }
    }

    override fun reset() {
        val settings = JDSettingsState.getInstance()

        settingsComponent?.let {
            it.hostText = settings.host
            it.socketText = settings.socket
            it.protocol = settings.protocol
        }
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    override fun getDisplayName(): String {
        return "Java Docker"
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}