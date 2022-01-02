package com.github.derteufelqwe.intellijjavaindockerplugin.core

import com.github.derteufelqwe.intellijjavaindockerplugin.MyBundle
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfiguration
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferInputStream
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferOutputStream
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.CollectCallback
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import okhttp3.internal.closeQuietly
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class JDProcess(private val docker: DockerClient, private val environment: ExecutionEnvironment) : Process() {

    private var exitValue = -1

    private val output = BufferInputStream()
    private val error = BufferInputStream()
    private val input = BufferOutputStream()
    private var execId: String? = null

    init {
        println("Creating process")
    }


    private fun start() {
        TimeUnit.SECONDS.sleep(1)

        val cmd = mutableListOf("java", "-classpath", "/javadeps/*:/javadeps/classes")

        if (environment.executor is DefaultDebugExecutor) {
            cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
        }

        cmd.add("test.Main")
        cmd.add("#${MyBundle.PROCESS_ID}")


        val resp = docker.execCreateCmd(Utils.getOptions(environment).hiddenContainerId!!)
                .withCmd(*cmd.toTypedArray())
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)
                .exec()

        docker.execStartCmd(resp.id)
            .withStdIn(input.input)
            .exec(object : ResultCallback<Frame> {

                override fun onStart(closeable: Closeable) {
                    println("Start")
                }

                override fun onNext(obj: Frame) {
                    when (obj.streamType) {
                        StreamType.STDOUT -> output.addData(obj.payload)
                        StreamType.STDERR -> error.addData(obj.payload)
                        else -> throw RuntimeException("Docker sent frame from " + obj.streamType)
                    }
                }

                override fun onError(throwable: Throwable) {
                    if (throwable is SocketException) {
                        if (throwable.message == "Socket closed") {
                            return
                        }
                    }

                    println("error")
                    throw RuntimeException("JDProcess failed to work", throwable)
                }

                override fun onComplete() {
                    println("complete")
                    exitValue = 0
                }

                @Throws(IOException::class)
                override fun close() {
                    println("close")
                }
            })

        this.execId = resp.id
    }

    override fun getOutputStream(): OutputStream {
        return input
    }

    override fun getInputStream(): InputStream {
        return output
    }

    override fun getErrorStream(): InputStream {
        return error
    }

    @Throws(InterruptedException::class)
    override fun waitFor(): Int {
        start()

        while (exitValue < 0) {
            TimeUnit.MILLISECONDS.sleep(100)
        }

        return exitValue
    }

    override fun exitValue(): Int {
        if (exitValue < 0) {
            throw IllegalThreadStateException()
        }

        return exitValue
    }

    override fun destroy() {
        shutdown("SIGTERM")
        println("destroy")
    }

    override fun destroyForcibly(): Process {
        shutdown("SIGKILL")
        println("destroy forcibly")
        return this
    }

    private fun shutdown(signal: String) {
        input.close()

        val containerID = Utils.getOptions(environment).hiddenContainerId!!
        val pids = getActivePIDs(containerID)

        pids.forEach {
            stopPid(it, containerID, signal)
        }

        output.close()
        error.close()
        exitValue = 0
    }

    private fun getActivePIDs(containerId: String): List<Int> {
        val res = docker.execCreateCmd(containerId)
            .withCmd("sh", "-c", "ps aux | grep ${MyBundle.PROCESS_ID}")
            .withAttachStdout(true)
            .withAttachStderr(true)
            .exec()

        val cb = docker.execStartCmd(res.id)
            .exec(CollectCallback())

        cb.await()
        val content = cb.result

        val RE_FIND = Pattern.compile("[a-zA-Z]+\\s+(\\d+).+")

        val pids = mutableListOf<Int>()

        for (line in content.split("\n")) {
            if ("java" in line) {
                val m = RE_FIND.matcher(line)
                if (m.matches()) {
                    pids.add(m.group(1).toInt())
                }
            }
        }

        return pids
    }

    private fun stopPid(pid: Int, containerId: String, signal: String) {
        val res = docker.execCreateCmd(containerId)
            .withCmd("kill", "-$signal", pid.toString())
            .withAttachStdout(true)
            .withAttachStderr(true)
            .exec()

        docker.execStartCmd(res.id)
            .exec(CollectCallback())
    }

}