package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.core.JDProcess
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.CollectCallback
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.model.Frame
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.newvfs.impl.FsRoot
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

class JDRunState(env: ExecutionEnvironment, private val docker: DockerClient, private val options: JDRunConfigurationOptions) : CommandLineState(env) {

    override fun startProcess(): ProcessHandler {
        if (!uploadDependencyFiles(options)) {
            throw RuntimeException("Dependency upload failed")
        }

        val process = JDProcess(docker, environment)
        val processHandler: ProcessHandler = JDProcessHandler(process, "command", StandardCharsets.UTF_8)

        ProcessTerminatedListener.attach(processHandler)
        processHandler.addProcessListener(TestListener(docker, options))
        processHandler.startNotify()

        return processHandler
    }

    private fun getAvailableFiles(containerId: String): List<String> {
        val buffer = StringBuilder()
        val done = AtomicBoolean(false)
        val error = arrayOf<Throwable?>(null)

        val resp: ExecCreateCmdResponse = docker.execCreateCmd(containerId)
            .withCmd("ls", "/javadeps")
            .withAttachStdout(true)
            .withAttachStderr(true)
            .exec()

        docker.execStartCmd(resp.id)
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

    private fun uploadDependencyFiles(options: JDRunConfigurationOptions): Boolean {
        var success = false

        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            try {
                val indicator = ProgressManager.getInstance().progressIndicator
                indicator.isIndeterminate = false
                indicator.text2 = ""
                val files = OrderEnumerator.orderEntries(environment.project).recursively().withoutSdk().classesRoots

                // Create the directory if required
                val exec = docker.execCreateCmd(options.hiddenContainerId!!)
                    .withWorkingDir("/")
                    .withCmd("mkdir", "-p", "/javadeps")
                    .exec()

                docker.execStartCmd(exec.id)
                    .exec(CollectCallback()).await()

                val existing = getAvailableFiles(options.hiddenContainerId!!)

                files.forEachIndexed{i, file ->
                    val path = (file as? FsRoot)?.path?.substring(0, file.getPath().length - 2) ?: file.path

                    if (file !is FsRoot || !existing.contains(file.getName())) {
                        indicator.text2 = file.name
                        docker.copyArchiveToContainerCmd(options.hiddenContainerId!!)
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
        }, "Uploading dependencies and source code", true, environment.project)

        return success
    }

}


/**
 * Custom ProcessHandler, which prevents an unnecessary error
 */
class JDProcessHandler : KillableProcessHandler {
    constructor(commandLine: GeneralCommandLine) : super(commandLine)
    constructor(process: Process, commandLine: GeneralCommandLine) : super(process, commandLine)
    constructor(commandLine: GeneralCommandLine, withMediator: Boolean) : super(commandLine, withMediator)
    constructor(process: Process, commandLine: String?) : super(process, commandLine)
    constructor(process: Process, commandLine: String?, charset: Charset) : super(process, commandLine, charset)
    constructor(process: Process, commandLine: String?, charset: Charset, filesToDelete: MutableSet<out File>?) : super(
        process,
        commandLine,
        charset,
        filesToDelete
    )


    override fun destroyProcessGracefully(): Boolean {
        // Without this the super method logs an error, which can't be prevented and causes the IDE to show an exception
        return false
    }
}


/**
 * Listens for process termination to kill the process if necessary
 */
class TestListener(private val docker: DockerClient, private val options: JDRunConfigurationOptions) : ProcessListener {

    override fun startNotified(event: ProcessEvent) {
        return
    }

    override fun processTerminated(event: ProcessEvent) {
        Utils.stopContainer(docker, options)
        println("Termination done")
    }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        return
    }


}