package com.github.derteufelqwe.intellijjavaindockerplugin.configs.old

import com.github.dockerjava.api.DockerClient
import com.intellij.execution.configurations.JavaCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.roots.ProjectRootManager

class TestingRunState(environment: ExecutionEnvironment, val docker: DockerClient) : JavaCommandLineState(environment) {

    override fun createJavaParameters(): JavaParameters {
        val params = JavaParameters()
        params.mainClass = "test.Main"

        val project = environment.project
        val manager = ProjectRootManager.getInstance(project)
        params.jdk = manager.projectSdk

        params.classPath.add("C:\\Users\\Arne\\Documents\\Git\\IntellijPluginTestProject\\target\\classes")

        return params
    }



}