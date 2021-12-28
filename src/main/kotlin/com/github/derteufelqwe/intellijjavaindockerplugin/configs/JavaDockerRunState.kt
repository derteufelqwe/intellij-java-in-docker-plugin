package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.core.JDProcess
import com.github.dockerjava.api.DockerClient
import com.intellij.execution.KillableProcess
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import java.nio.charset.StandardCharsets

class JavaDockerRunState(env: ExecutionEnvironment, val docker: DockerClient) : CommandLineState(env) {

    override fun getRunnerSettings(): RunnerSettings? {
        return super.getRunnerSettings()
    }

    override fun startProcess(): ProcessHandler {
        val process = JDProcess(docker, environment.project)
        val processHandler: ProcessHandler = BaseOSProcessHandler(process, "command", StandardCharsets.UTF_8)

        ProcessTerminatedListener.attach(processHandler)
        processHandler.startNotify()

        return processHandler
    }
}