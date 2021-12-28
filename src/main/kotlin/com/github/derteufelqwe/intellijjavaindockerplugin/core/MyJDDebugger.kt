package com.github.derteufelqwe.intellijjavaindockerplugin.core

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfiguration
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.old.MyLocatableConfig
import com.intellij.debugger.engine.DebuggerUtils
import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor

class MyJDDebugger : GenericDebuggerRunner() {

    override fun getRunnerId(): String {
        return "Arne-runner-id"
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId == "Debug" && profile is JDRunConfiguration
    }

    override fun execute(environment: ExecutionEnvironment) {
        super.execute(environment)
    }

    override fun createContentDescriptor(
        state: RunProfileState,
        environment: ExecutionEnvironment
    ): RunContentDescriptor? {
        val executor = environment.executor

        if (executor is DefaultDebugExecutor) {
            val port = DebuggerUtils.getInstance().findAvailableDebugAddress(true)
//            val stateWithDebug = (state as JavaCommandLine)
//            stateWithDebug.javaParameters.vmParametersList.addParametersString("-Xdebug")
//            stateWithDebug.javaParameters.vmParametersList.addParametersString("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$port")
            val conn = RemoteConnection(true, "ubuntu1", "5005", false)

            return super.attachVirtualMachine(state, environment, conn, true)
        }

        return super.createContentDescriptor(state, environment)
    }

}