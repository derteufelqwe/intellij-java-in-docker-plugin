package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.*
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import java.util.concurrent.TimeUnit

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


        if (!options.useExistingContainer) {
            if (!setupContainer(options)) {
                return null
            }

        } else {
            if (options.containerId == null || options.containerId == "") {
                Utils.showBalloonPopup(project, "You must set an existing container id")
                return null
            }

            if (!checkContainerExists(options.containerId!!)) {
                Utils.showBalloonPopup(project, "Existing container does not actually exist")
                return null
            }

            val resp = inspectExistingContainer(options.containerId!!)
            if (!resp.success) {
                Utils.showBalloonPopup(project, "Existing container doesnt meet requirements")
                return null
            }

        }

        return JDRunState(environment, factory.docker, options)
    }


    private fun createContainer(options: JDRunConfigurationOptions): ContainerCreateResponse {
        if (options.dockerImage == null) {
            Utils.showError("You must configure a docker image")
            return ContainerCreateResponse(false, null)
        }

        // Stop the old container if you just removed the 'reuse container' flag
        Utils.stopContainer(docker, options)

        val container = factory.docker.createContainerCmd(options.dockerImage!!)
            .withTty(true)  // Prevents the container from stopping
            .withExposedPorts(ExposedPort(5005, InternetProtocol.TCP))
            .withHostConfig(
                HostConfig()
                    .withPortBindings(
                        PortBinding(
                            Ports.Binding("0.0.0.0", ""),
                            ExposedPort(5005, InternetProtocol.TCP)
                        )
                    )
            )
            .exec()

        factory.docker.startContainerCmd(container.id)
            .exec()

        return ContainerCreateResponse(true, container.id)
    }

    private fun checkContainer(containerId: String): ContainerCheckResponse {
        try {
            val resp = factory.docker.inspectContainerCmd(containerId).exec()
            val ports = resp.networkSettings.ports.toPrimitive()

            if (ports.isEmpty()) {
                Utils.showError("Container $containerId does not expose any ports")
                return ContainerCheckResponse(false)
            }

            if (!ports.containsKey("5005/tcp")) {
                Utils.showError("Container $containerId must expose port 5005 for debugging")
                return ContainerCheckResponse(false)
            }

            return ContainerCheckResponse(
                true,
                resp.id,
                ports!!["5005/tcp"]!![0]["HostPort"]!!.toInt()
            )

        } catch (e: NotFoundException) {
            return ContainerCheckResponse(false)
        }
    }

    private fun checkContainerExists(containerId: String): Boolean {
        try {
            val response = docker.inspectContainerCmd(containerId).exec()
            return response.state.running ?: false

        } catch (e: NotFoundException) {
            return false
        }
    }

    private fun setupContainer(options: JDRunConfigurationOptions): Boolean {
        var tmpContainerId: String? = null

        // Reuse the container
        if (options.reuseContainer) {
            // Container exists
            if (options.hiddenContainerId != null && options.hiddenContainerId != "") {
                if (!checkContainerExists(options.hiddenContainerId!!)) {
                    Utils.showBalloonPopup(project, "Container ${options.hiddenContainerId?.substring(0, 16)} doesn't exist anymore.")
                } else {
                    return true
                }
            }
        }

        // Container must be created
        val createResp = createContainer(options)
        if (!createResp.success) {
            Utils.showError("Failed to create container.")
            return false

        } else {
            tmpContainerId = createResp.id
        }

        // Check if the creation succeeded
        val checkResp = checkContainer(tmpContainerId!!)
        if (!checkResp.success) {
            Utils.showError("Failed to start container. Did it shutdown immediately?")
            return false

        } else {
            options.hiddenContainerId = tmpContainerId
            options.port = checkResp.port!!
        }

        return true
    }

    private fun inspectExistingContainer(containerId: String): ContainerInspectResponse {
        val resp = docker.inspectContainerCmd(containerId).exec()

        return ContainerInspectResponse(true, "")
    }

}

private data class ContainerCreateResponse(val success: Boolean, val id: String?) {
    constructor(success: Boolean) : this(success, null)
}

private data class ContainerCheckResponse(val success: Boolean, val id: String?, val port: Int?) {
    constructor(success: Boolean) : this(success, null, null)
}

private data class ContainerInspectResponse(val success: Boolean, val msg: String)