package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.CollectCallback
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.*
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.KillableProcess
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.progress.util.AbstractProgressIndicatorBase
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.vfs.newvfs.impl.FsRoot
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.Time
import java.awt.TrayIcon
import java.io.Closeable
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

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

        if (!setupContainer(options)) {
            return null
        }

        return JDRunState(environment, factory.docker, options)
    }


    private fun createContainer(options: JDRunConfigurationOptions): ContainerCreateResponse {
        if (options.dockerImage == null) {
            Utils.showError("You must configure a docker image")
            return ContainerCreateResponse(false, null)
        }

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
                    val statusBar = WindowManager.getInstance().getStatusBar(project);
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder("Container ${options.hiddenContainerId?.substring(0, 16)} doesn't exist anymore.",
                                MessageType.WARNING, null)
                            .setFadeoutTime(3000)
                            .createBalloon()
                            .show(RelativePoint.getCenterOf(statusBar.component), Balloon.Position.above)
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

}

private data class ContainerCreateResponse(val success: Boolean, val id: String?) {
    constructor(success: Boolean) : this(success, null)
}
private data class ContainerCheckResponse(val success: Boolean, val id: String?, val port: Int?) {
    constructor(success: Boolean) : this(success, null, null)
}

private class TTask(project: Project) : Task.Backgroundable(project, "Title") {
    override fun run(indicator: ProgressIndicator) {
        println("TTask start")
        TimeUnit.SECONDS.sleep(5)
        println("TTask end")
    }
}