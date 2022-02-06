package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.*
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType

class JDRunConfiguration(project: Project, private val factory: JDConfigurationFactory, name: String) :
    LocatableConfigurationBase<JDRunConfigurationOptions>(project, factory, name),
    RunProfileWithCompileBeforeLaunchOption {

    val docker = factory.docker;


    override fun getOptions(): JDRunConfigurationOptions {
        return super.getOptions() as JDRunConfigurationOptions
    }

    /**
     * Alternative getter for the options
     */
    fun getOptions2(): JDRunConfigurationOptions {
        return options
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return JDForm(project)
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val options = Utils.getOptions(environment)
        val containerId: String?

        // Create a container
        if (!options.useExistingContainer) {
            val setupResponse = setupContainer(options)
            if (!setupResponse.success) {
                return null
            }
            containerId = setupResponse.id

        // Use an existing container
        } else {
            if (options.containerId == null || options.containerId == "") {
                Utils.showBalloonPopup(project, "You must set an existing container id")
                return null
            }

            if (!checkContainerExists(options.containerId!!)) {
                Utils.showBalloonPopup(project, "Existing container does not actually exist")
                return null
            }
            containerId = options.containerId
        }

        // ### Container up and running. Check its state ###

        // Check and extract required information from container
        val resp = inspectContainer(containerId!!)
        if (!resp.success) {
            Utils.showBalloonPopup(project, "Existing container doesn't meet requirements.")
            return null
        }

        options.hiddenContainerId = containerId
        options.debuggerPort = resp.debuggerPort
        options.rsyncPort = resp.rsyncPort

        return JDRunState(environment, factory.docker, options)
    }

    /**
     * Creates and starts a new java container
     */
    private fun createContainer(options: JDRunConfigurationOptions): ContainerCreateResponse {
        if (options.dockerImage == null) {
            Utils.showBalloonPopup(project, "You must configure a docker image", type = MessageType.ERROR)
            return ContainerCreateResponse(false, null)
        }

        // Stop the old container if you just removed the 'reuse container' flag
        Utils.stopContainer(docker, options)

        val additionalPorts = options.parsedExposedPorts

        val container = factory.docker.createContainerCmd(options.dockerImage!!)
            .withTty(true)  // Prevents the container from stopping
            .withLabels(mapOf("creator" to "JavaInDocker"))
            .withExposedPorts(
                ExposedPort(5005, InternetProtocol.TCP),
                ExposedPort(12000, InternetProtocol.TCP),
                *additionalPorts.map { it.getExposedPort() }.toTypedArray() // Add user specified ports (1)
            )
            .withHostConfig(
                HostConfig()
                    .withPortBindings(
                        PortBinding(
                            Ports.Binding("0.0.0.0", ""),
                            ExposedPort(5005, InternetProtocol.TCP)
                        ),
                        PortBinding(
                            Ports.Binding("0.0.0.0", ""),
                            ExposedPort(12000, InternetProtocol.TCP)
                        ),
                        *additionalPorts.map { it.getPortBinding() }.toTypedArray() // Add user specified ports (2)
                    )
            )
            .exec()

        factory.docker.startContainerCmd(container.id)
            .exec()

        return ContainerCreateResponse(true, container.id)
    }

    /**
     * Inspects an existing container and checks the required config
     */
    private fun inspectContainer(containerId: String): ContainerInspectResponse {
        try {
            val resp = factory.docker.inspectContainerCmd(containerId).exec()
            val ports = resp.networkSettings.ports.toPrimitive()

            if (ports.isEmpty()) {
                Utils.showBalloonPopup(project, "Container $containerId does not expose any ports", type = MessageType.ERROR)
                return ContainerInspectResponse(false)
            }

            if (!ports.containsKey("5005/tcp")) {
                Utils.showBalloonPopup(project, "Container $containerId must expose port 5005 for debugging", type = MessageType.ERROR)
                return ContainerInspectResponse(false)
            }

            if (!ports.containsKey("12000/tcp")) {
                Utils.showBalloonPopup(project, "Container $containerId must expose port 12000 for rsync", type = MessageType.ERROR)
                return ContainerInspectResponse(false)
            }

            return ContainerInspectResponse(
                true,
                resp.id,
                ports["5005/tcp"]!![0]["HostPort"]!!.toInt(),
                ports["12000/tcp"]!![0]["HostPort"]!!.toInt(),
            )

        } catch (e: NotFoundException) {
            return ContainerInspectResponse(false)
        }
    }

    /**
     * Checks if a container exists and is running
     */
    private fun checkContainerExists(containerId: String): Boolean {
        try {
            val response = docker.inspectContainerCmd(containerId).exec()
            return response.state.running ?: false

        } catch (e: NotFoundException) {
            return false
        }
    }

    /**
     * Creates a new container or reuses the existing one
     */
    private fun setupContainer(options: JDRunConfigurationOptions): ContainerSetupResponse {
                // Reuse the container
        if (options.reuseContainer) {
            // Container exists
            if (options.hiddenContainerId != null && options.hiddenContainerId != "") {
                if (!checkContainerExists(options.hiddenContainerId!!)) {
                    Utils.showBalloonPopup(project,"Container ${options.hiddenContainerId?.substring(0, 16)} doesn't exist anymore. Creating a new one.")

                } else {
                    return ContainerSetupResponse(true, options.hiddenContainerId)
                }
            }
        }

        // Container must be created
        val createResp = createContainer(options)
        if (!createResp.success) {
            Utils.showError("Failed to create container.")
            return ContainerSetupResponse(false)

        } else {
            return ContainerSetupResponse(true, createResp.id)
        }
    }

    /**
     * Shows errors at the bottom
     */
    override fun checkConfiguration() {
        if (options.mainClass == null || options.mainClass == "") {
            throw RuntimeConfigurationError("Main class can't be empty")
        }

        if (options.exposedPorts != null && options.exposedPorts != "") {
            Utils.parseExposedPorts(options.exposedPorts!!)
        }

    }

}

private data class ContainerCreateResponse(val success: Boolean, val id: String?) {
    constructor(success: Boolean) : this(success, null)
}

private data class ContainerSetupResponse(val success: Boolean, val id: String?) {
    constructor(success: Boolean) : this(success, null)
}

private data class ContainerInspectResponse(val success: Boolean, val id: String?, val debuggerPort: Int, val rsyncPort: Int) {
    constructor(success: Boolean) : this(success, null, -1, -1)
}
