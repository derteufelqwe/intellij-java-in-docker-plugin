package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationBase
import com.intellij.execution.ShortenCommandLine
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.module.Module
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class MyConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    val docker = createDockerConnection()


    override fun getId(): String {
        return MyRunConfigType.ID
    }

//    override fun getOptionsClass(): Class<out BaseState> {
//        return MyRunConfigurationOptions::class.java
//    }

    override fun createConfiguration(name: String?, template: RunConfiguration): RunConfiguration {
        return super.createConfiguration(name, template)
    }

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return JavaDockerRunConfiguration(
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

//        val client = ApacheDockerHttpClient.Builder()
//            .dockerHost(config.dockerHost)
//            .sslConfig(config.sslConfig)
//            .maxConnections(20)
//            .connectionTimeout(Duration.ofSeconds(30))
//            .responseTimeout(Duration.ofSeconds(45))
//            .build()

        val client = OkDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .build()

        return DockerClientImpl.getInstance(config, client)
    }

}
