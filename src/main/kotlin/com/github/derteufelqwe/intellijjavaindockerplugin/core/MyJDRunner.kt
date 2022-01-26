package com.github.derteufelqwe.intellijjavaindockerplugin.core

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfiguration
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.impl.DefaultJavaProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.TargetEnvironmentAwareRunProfileState
import com.intellij.execution.ui.RunContentDescriptor
import org.jetbrains.concurrency.Promise


class MyJDRunner : DefaultJavaProgramRunner() {

    override fun getRunnerId(): String {
        return "Arne-runner-id"
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId == "Run" && profile is JDRunConfiguration
    }

    override fun execute(environment: ExecutionEnvironment) {
        super.execute(environment)
    }

    override fun doExecute(state: RunProfileState, env: ExecutionEnvironment): RunContentDescriptor {
        return super.doExecute(state, env)
    }

    override fun doExecuteAsync(
        state: TargetEnvironmentAwareRunProfileState,
        env: ExecutionEnvironment
    ): Promise<RunContentDescriptor?> {
        return super.doExecuteAsync(state, env)
    }
}