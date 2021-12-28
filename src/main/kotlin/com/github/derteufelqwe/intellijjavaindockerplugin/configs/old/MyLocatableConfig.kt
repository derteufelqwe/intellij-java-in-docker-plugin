package com.github.derteufelqwe.intellijjavaindockerplugin.configs.old

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.*
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.old.TestingRunState
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class MyLocatableConfig(project: Project, factory: JDConfigurationFactory, name: String?) :
    LocatableConfigurationBase<JDRunConfigurationOptions>(project, factory, name), RunProfileWithCompileBeforeLaunchOption {


    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
//        return JavaDockerRunState(environment, (factory as MyConfigurationFactory).docker)
        return TestingRunState(environment, (factory as JDConfigurationFactory).docker)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return JDForm()
    }


}