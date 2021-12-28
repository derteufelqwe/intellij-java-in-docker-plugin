package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.MyBundle
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.CollectCallback
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.model.*
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
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
import java.lang.IndexOutOfBoundsException
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

class JDRunConfiguration(project: Project, private val factory: JDConfigurationFactory, name: String) :
    LocatableConfigurationBase<JDRunConfigurationOptions>(project, factory, name), RunProfileWithCompileBeforeLaunchOption {
        

    override fun getOptions(): JDRunConfigurationOptions {
        return super.getOptions() as JDRunConfigurationOptions
    }


    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return JDForm()
    }


    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
        val options = (environment.runProfile as JDRunConfiguration).options
        val en = OrderEnumerator.orderEntries(project).recursively()
        var id = options.containerId
        var port = -1

        try {
            if (!options.reuseContainer || options.containerId == null) {
                val createResp = createContainer(options)
                if (!createResp.success) {
                    return null
                } else {
                    id = createResp.id
                }
            }

            val checkResp = checkContainer(id!!)
            if (!checkResp.success) {
                return null
            } else {
                port = checkResp.port ?: -2
                options.containerId = id
            }

        } catch (e: IndexOutOfBoundsException) {
            return null
        }

        var success = false
        val r = ProgressManager.getInstance().runProcessWithProgressSynchronously({
            try {
                val indicator = ProgressManager.getInstance().progressIndicator

                indicator.isIndeterminate = false
                indicator.text2 = ""
                val files = en.withoutSdk().classesRoots

                // Create the directory
                val exec = factory.docker.execCreateCmd(id)
                    .withWorkingDir("/")
                    .withCmd("mkdir", "-p", "/javadeps")
                    .exec()

                val existing = getAvailableFiles(id)

                factory.docker.execStartCmd(exec.id)
                    .exec(CollectCallback()).await()

                for (i in files.indices) {
                    val file = files[i]
                    val path = (file as? FsRoot)?.path?.substring(0, file.getPath().length - 2) ?: file.path

                    if (file !is FsRoot || !existing.contains(file.getName())) {
                        indicator.text2 = file.name
                        factory.docker.copyArchiveToContainerCmd(id)
                            .withHostResource(path)
                            .withRemotePath("/javadeps")
                            .exec()
                    }
                    indicator.fraction = i / files.size.toDouble()
                }

                success = true

            } catch (e: Exception) {
                Notifications.Bus.notify(Notification("Java Docker", "Uploading dependencies failed", NotificationType.ERROR))
                throw e
            }
        }, "Uploading dependencies and source code", true, project)

        if (!success) {
            return null
        }

        return JDRunState(environment, factory.docker)
    }

    private fun createContainer(options: JDRunConfigurationOptions): ContainerCreateResponse {
        if (options.dockerImage == null) {
            Notifications.Bus.notify(Notification("Java Docker", "You must configure a docker image", NotificationType.ERROR))
            return ContainerCreateResponse(false, null)
        }

        val container = factory.docker.createContainerCmd(options.dockerImage!!)
            .withTty(true)  // Prevents the container from stopping
            .withExposedPorts(ExposedPort(5005, InternetProtocol.TCP))
            .withHostConfig(HostConfig()
                .withPortBindings(PortBinding(Ports.Binding("0.0.0.0", ""), ExposedPort(5005, InternetProtocol.TCP))))
            .exec()

        factory.docker.startContainerCmd(container.id)
            .exec()

        return ContainerCreateResponse(true, container.id)
    }

    private fun checkContainer(id: String): ContainerCheckResponse {
        val resp = factory.docker.inspectContainerCmd(id).exec()

        return ContainerCheckResponse(true, resp.id, resp.networkSettings.ports.toPrimitive()!!["5005/tcp"]!![0]["HostPort"]!!.toInt())
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


}

private data class ContainerCreateResponse(val success: Boolean, val id: String?)
private data class ContainerCheckResponse(val success: Boolean, val id: String?, val port: Int?)
