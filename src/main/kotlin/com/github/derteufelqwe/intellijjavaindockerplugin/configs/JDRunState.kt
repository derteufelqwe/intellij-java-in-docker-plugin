package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.core.JDProcess
import com.github.dockerjava.api.DockerClient
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import java.nio.charset.StandardCharsets

class JDRunState(env: ExecutionEnvironment, val docker: DockerClient) : CommandLineState(env) {

    override fun startProcess(): ProcessHandler {
        val process = JDProcess(docker, environment)
        val processHandler: ProcessHandler = BaseOSProcessHandler(process, "command", StandardCharsets.UTF_8)

        ProcessTerminatedListener.attach(processHandler)
        processHandler.startNotify()

        return processHandler
    }

}
