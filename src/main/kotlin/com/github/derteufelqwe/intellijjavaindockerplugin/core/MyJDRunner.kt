package com.github.derteufelqwe.intellijjavaindockerplugin.core

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.runconfig.MyLocatableConfig
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.impl.DefaultJavaProgramRunner
import com.intellij.execution.runners.ExecutionEnvironment

class MyJDRunner : DefaultJavaProgramRunner() {

    override fun getRunnerId(): String {
        return "Arne-runner-id"
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return executorId == "Run" && profile is MyLocatableConfig
    }

    override fun execute(environment: ExecutionEnvironment) {
        super.execute(environment)
    }
}