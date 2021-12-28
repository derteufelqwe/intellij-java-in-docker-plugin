package com.github.derteufelqwe.intellijjavaindockerplugin.configs

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

    val docker = createDockerConnection()


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


    private fun createDockerConnection(): DockerClient {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("tcp://ubuntu1:2375")
            .withDockerTlsVerify(false)
            .build()

        val client = OkDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .connectTimeout(30)
            .build()

        return DockerClientImpl.getInstance(config, client)
    }

}
