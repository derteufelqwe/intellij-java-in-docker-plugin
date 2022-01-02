package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.CollectCallback
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.model.*
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunProfileWithCompileBeforeLaunchOption
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.vfs.newvfs.impl.FsRoot
import java.io.Closeable
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

class JDRunConfiguration(project: Project, private val factory: JDConfigurationFactory, name: String) :
    LocatableConfigurationBase<JDRunConfigurationOptions>(project, factory, name),
    RunProfileWithCompileBeforeLaunchOption {


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
        return JDForm()
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val options = Utils.getOptions(environment)

        try {
            if (!setupContainer(options)) {
                return null
            }

        } catch (e: IndexOutOfBoundsException) {
            return null
        }


        if (!uploadDependencyFiles(options)) {
            return null
        }

        return JDRunState(environment, factory.docker)
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
    }

    private fun getAvailableFiles(containerId: String): List<String> {
        val buffer = StringBuilder()
        val done = AtomicBoolean(false)
        val error = arrayOf<Throwable?>(null)

        val resp: ExecCreateCmdResponse = factory.docker.execCreateCmd(containerId)
            .withCmd("ls", "/javadeps")
            .withAttachStdout(true)
            .withAttachStderr(true)
            .exec()

        factory.docker.execStartCmd(resp.id)
            .exec(object : ResultCallback<Frame> {
                override fun onStart(closeable: Closeable) {}

                override fun onNext(obj: Frame) {
                    buffer.append(String(obj.payload))
                }

                override fun onError(throwable: Throwable) {
                    error[0] = throwable
                    done.set(true)
                }

                override fun onComplete() {
                    done.set(true)
                }

                @Throws(IOException::class)
                override fun close() {
                    done.set(true)
                }
            })

        while (!done.get()) {
            try {
                TimeUnit.MILLISECONDS.sleep(10)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (error[0] != null) {
            throw RuntimeException("Existing deps download failed.", error[0])
        }

        return Arrays.stream(buffer.toString().split("\n".toRegex()).toTypedArray())
            .map { obj: String -> obj.strip() }
            .collect(Collectors.toList())
    }

    private fun setupContainer(options: JDRunConfigurationOptions): Boolean {
        var tmpContainerId: String? = null

        // Create container if required
        if (!options.reuseContainer || options.containerId == null || options.containerId == "") {
            val createResp = createContainer(options)
            if (!createResp.success) {
                return false
            } else {
                tmpContainerId = createResp.id
            }
        } else {
            tmpContainerId = options.containerId
        }

        val checkResp = checkContainer(tmpContainerId!!)
        if (!checkResp.success) {
            return false
        } else {
            options.hiddenContainerId = tmpContainerId
            options.port = checkResp.port!!
        }

        return true
    }

    private fun uploadDependencyFiles(options: JDRunConfigurationOptions): Boolean {
        var success = false

        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            try {
                val indicator = ProgressManager.getInstance().progressIndicator
                indicator.isIndeterminate = false
                indicator.text2 = ""
                val files = OrderEnumerator.orderEntries(project).recursively().withoutSdk().classesRoots

                // Create the directory if required
                val exec = factory.docker.execCreateCmd(options.hiddenContainerId!!)
                    .withWorkingDir("/")
                    .withCmd("mkdir", "-p", "/javadeps")
                    .exec()

                factory.docker.execStartCmd(exec.id)
                    .exec(CollectCallback()).await()

                val existing = getAvailableFiles(options.hiddenContainerId!!)

                files.forEachIndexed{i, file ->
                    val path = (file as? FsRoot)?.path?.substring(0, file.getPath().length - 2) ?: file.path

                    if (file !is FsRoot || !existing.contains(file.getName())) {
                        indicator.text2 = file.name
                        factory.docker.copyArchiveToContainerCmd(options.hiddenContainerId!!)
                            .withHostResource(path)
                            .withRemotePath("/javadeps")
                            .exec()
                    }
                    indicator.fraction = i / files.size.toDouble()
                }

                success = true

            } catch (e: Exception) {
                Notifications.Bus.notify(
                    Notification(
                        "Java Docker",
                        "Uploading dependencies failed",
                        NotificationType.ERROR
                    )
                )
                throw e
            }
        }, "Uploading dependencies and source code", true, project)

        return success
    }

}

private data class ContainerCreateResponse(val success: Boolean, val id: String?) {
    constructor(success: Boolean) : this(success, null)
}
private data class ContainerCheckResponse(val success: Boolean, val id: String?, val port: Int?) {
    constructor(success: Boolean) : this(success, null, null)
}
