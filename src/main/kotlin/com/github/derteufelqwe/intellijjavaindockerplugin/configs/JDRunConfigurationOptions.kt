package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.PortInfo
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.VolumeInfo
import com.intellij.execution.configurations.LocatableRunConfigurationOptions

/**
 * Stores settings
 */
class JDRunConfigurationOptions : LocatableRunConfigurationOptions() {

    // The class to run
    private val pMainClass = string("").provideDelegate(this, "mainClass")
    var mainClass: String?
        get() = pMainClass.getValue(this)
        set(value) = pMainClass.setValue(this, value?.trim())

    // JVM arguments
    private val pJvmArgs = string("").provideDelegate(this, "jvmArgs")
    var jvmArgs: String?
        get() = pJvmArgs.getValue(this)
        set(value) = pJvmArgs.setValue(this, value?.trim())

    // Java runtime parameters
    private val pJavaParams = string("").provideDelegate(this, "javaParams")
    var javaParams: String?
        get() = pJavaParams.getValue(this)
        set(value) = pJavaParams.setValue(this, value?.trim())



    // The docker image to spin up if required
    private val pDockerImage = string("").provideDelegate(this, "dockerImage")
    var dockerImage: String?
        get() = pDockerImage.getValue(this)
        set(value) = pDockerImage.setValue(this, value?.trim())

    // Unchecked = spin up a new container each run
    private val pReuseContainer = property(false).provideDelegate(this, "reuseContainer")
    var reuseContainer: Boolean
        get() = pReuseContainer.getValue(this)
        set(value) = pReuseContainer.setValue(this, value)

    // Checked = add --rm flag to container
    private val pRemoveContainer = property(false).provideDelegate(this, "removeContainer")
    var removeContainerOnStop: Boolean
        get() = pRemoveContainer.getValue(this)
        set(value) = pRemoveContainer.setValue(this, value)

    // The current containerID / enter a container ID if you want to use an existing one
    private val pContainerId = string("").provideDelegate(this, "containerId")
    var containerId: String?
        get() = pContainerId.getValue(this)
        set(value) = pContainerId.setValue(this, value?.trim())

    // Hidden containerID, which stores the currently running containers ID
    private val pHiddenContainerId = string("").provideDelegate(this, "hiddenContainerId")
    var hiddenContainerId: String?
        get() = pHiddenContainerId.getValue(this)
        set(value) = pHiddenContainerId.setValue(this, value?.trim())



    // Configures additional exposed ports
    private val pExposedPorts = string("").provideDelegate(this, "exposedPorts")
    var exposedPorts: String?
        get() = pExposedPorts.getValue(this)
        set(value) = pExposedPorts.setValue(this, value?.trim())

    val parsedExposedPorts: List<PortInfo>
        get() = if (exposedPorts != null) Utils.parseExposedPorts(exposedPorts!!) else listOf()

    // Configures additional container mounts
    private val pMounts = string("").provideDelegate(this, "mounts")
    var mounts: String?
        get() = pMounts.getValue(this)
        set(value) = pMounts.setValue(this, value?.trim())

    val parsedMounts: List<VolumeInfo>
        get() = if (mounts != null) Utils.parseAdditionalVolumes(mounts!!) else listOf()


    // Stores the port of the running run configuration
    private val pDebuggerPort = property(-1).provideDelegate(this, "debuggerPort")
    var debuggerPort: Int
        get() = pDebuggerPort.getValue(this)
        set(value) = pDebuggerPort.setValue(this, value)

    // Stores the rsync port of the running run configuration
    private val pRsyncPort = property(-1).provideDelegate(this, "rsyncPort")
    var rsyncPort: Int
        get() = pRsyncPort.getValue(this)
        set(value) = pRsyncPort.setValue(this, value)

    // Checked = use the existing container
    private val pUseExistingContainer = property(false).provideDelegate(this, "useExistingContainer")
    var useExistingContainer: Boolean
        get() = pUseExistingContainer.getValue(this)
        set(value) = pUseExistingContainer.setValue(this, value)

}