package com.github.derteufelqwe.intellijjavaindockerplugin.core

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.runconfig.MyLocatableConfig
import com.intellij.debugger.engine.DebuggerUtils
import com.intellij.debugger.impl.GenericDebuggerRunner
import com.intellij.execution.configurations.JavaCommandLine
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.impl.DefaultJavaProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil

class MyJDDebugger : GenericDebuggerRunner() {

    override fun getRunnerId(): String {
        return "Arne-runner-id"
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId == "Debug" && profile is MyLocatableConfig
    }

    override fun execute(environment: ExecutionEnvironment) {
        super.execute(environment)
    }

    override fun createContentDescriptor(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        val executor = environment.executor

        if (executor is DefaultDebugExecutor) {
            val port = DebuggerUtils.getInstance().findAvailableDebugAddress(true)
            val stateWithDebug = (state as JavaCommandLine)
            stateWithDebug.javaParameters.vmParametersList.addParametersString("-Xdebug")
            stateWithDebug.javaParameters.vmParametersList.addParametersString("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=$port")
            val conn = RemoteConnection(true, "127.0.0.1", port, false)

            return super.attachVirtualMachine(stateWithDebug, environment, conn, true)
        }

        return super.createContentDescriptor(state, environment)
    }

}