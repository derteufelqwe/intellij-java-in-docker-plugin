package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.intellij.execution.configurations.LocatableRunConfigurationOptions

/**
 * Stores settings
 */
class JDRunConfigurationOptions : LocatableRunConfigurationOptions() {

    // The class to run
    private val pMainClass = string("").provideDelegate(this, "mainClass")
    var mainClass: String?
        get() = pMainClass.getValue(this)
        set(value) = pMainClass.setValue(this, value)

    // The docker image to spin up if required
    private val pDockerImage = string("").provideDelegate(this, "dockerImage")
    var dockerImage: String?
        get() = pDockerImage.getValue(this)
        set(value) = pDockerImage.setValue(this, value)

    // Unchecked = spin up a new container each run
    private val pReuseContainer = property(false).provideDelegate(this, "reuseContainer")
    var reuseContainer: Boolean
        get() = pReuseContainer.getValue(this)
        set(value) = pReuseContainer.setValue(this, value)

    // The current containerID / enter a container ID if you want to use an existing one
    private val pContainerId = string("").provideDelegate(this, "containerId")
    var containerId: String?
        get() = pContainerId.getValue(this)
        set(value) = pContainerId.setValue(this, value)

}