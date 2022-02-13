package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.settings.JDSettingsState
import com.github.derteufelqwe.intellijjavaindockerplugin.settings.Protocol
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

class JDConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    val docker: DockerClient

    init {
        val settings = JDSettingsState.getInstance()
        val host = when(settings.protocol) {
            Protocol.UNIX -> settings.socket
            Protocol.TCP -> settings.host
        }
        docker = Utils.createDockerConnection("${settings.protocol.text}://$host")
    }

    override fun getId(): String {
        return JDRunConfigType.ID
    }

    override fun getOptionsClass(): Class<out BaseState> {
        return JDRunConfigurationOptions::class.java
    }

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return JDRunConfiguration(
            project,
            this,
            "Arne"
        );
    }


}
