package com.github.derteufelqwe.intellijjavaindockerplugin.core

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfiguration
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
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
            val options = Utils.getOptions(environment)
            val port = options.debuggerPort

            if (port <= 0) {
                throw RuntimeException("Port is somehow not set.")
            }

            val conn = RemoteConnection(true, "ubuntu1", port.toString(), false)

            return super.attachVirtualMachine(state, environment, conn, true)
        }

        return super.createContentDescriptor(state, environment)
    }

}