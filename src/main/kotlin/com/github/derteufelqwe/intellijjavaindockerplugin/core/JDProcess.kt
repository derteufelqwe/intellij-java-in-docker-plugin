package com.github.derteufelqwe.intellijjavaindockerplugin.core

import com.github.derteufelqwe.intellijjavaindockerplugin.MyBundle
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferInputStream
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferOutputStream
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.CollectCallback
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.lang.folding.FoldingBuilderEx
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

class JDProcess(private val docker: DockerClient, private val environment: ExecutionEnvironment, private val classPathFiles: List<String>) : Process() {

    private val RE_EXTRACT_PID = Pattern.compile("[a-zA-Z]+\\s+(\\d+).+")
    private val RE_FIND_MAINCLASS = Pattern.compile(".+ (.+)\$")

    private var exitValue = AtomicInteger(-1)
    private val options = Utils.getOptions(environment)

    private val output = BufferInputStream()
    private val error = BufferInputStream()
    private val input = BufferOutputStream()
    private var execId: String? = null
    private var stopCount = 0

    init {
        println("Creating process")
    }


    private fun start() {
        val classPath = classPathFiles
            .map { it.split("/").last() }
            .map { "/javadeps/$it" }
            .joinToString(":")

        val cmd = mutableListOf("java", "-Dfile.encoding=UTF-8", MyBundle.JVM_PROCESS_IDENTIFIER, "-classpath", classPath)

        // Add JVM params
        options.jvmArgs?.let {
            cmd.addAll(it.split(" "))
        }

        if (environment.executor is DefaultDebugExecutor) {
            cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
        }

        cmd.add(options.mainClass!!)

        // Add java arguments
        options.javaParams?.let {
            cmd.add(it)
        }

        output.addData(cmd.joinToString(" ") + "\n")


        val resp = docker.execCreateCmd(Utils.getOptions(environment).hiddenContainerId!!)
            .withCmd(*cmd.toTypedArray())
            .withAttachStdin(true)
            .withAttachStdout(true)
            .withAttachStderr(true)
            .exec()

        docker.execStartCmd(resp.id)
            .withStdIn(input.input)
            .exec(JavaRunExecCallback(exitValue, output, error))

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

        while (exitValue.get() < 0) {
            TimeUnit.MILLISECONDS.sleep(100)
        }

        return exitValue.get()
    }

    override fun exitValue(): Int {
        if (exitValue.get() < 0) {
            throw IllegalThreadStateException()
        }

        return exitValue.get()
    }

    override fun destroy() {
        if (stopCount == 0) {
            shutdown("SIGTERM")
            println("destroy")

        } else {
            shutdown("SIGKILL")
            println("destroy kill")
        }

        stopCount += 1
    }

    private fun shutdown(signal: String) {
        val containerID = Utils.getOptions(environment).hiddenContainerId!!
        val pids = getActivePIDs(containerID)

        pids.forEach {
            stopPid(it, containerID, signal)
        }
    }

    private fun getActivePIDs(containerId: String): List<Int> {
        val res = docker.execCreateCmd(containerId)
            .withCmd("sh", "-c", "ps aux | grep \\\\${MyBundle.JVM_PROCESS_IDENTIFIER}")
            .withAttachStdout(true)
            .withAttachStderr(true)
            .exec()

        val cb = docker.execStartCmd(res.id)
            .exec(CollectCallback())

        cb.await()

        return cb.result.split("\n")
            .map { it.trim() }
            .filter { "java" in it }
            .filter { val m = RE_FIND_MAINCLASS.matcher(it); m.matches() && m.group(1).equals(options.mainClass) }
            .map { val m = RE_EXTRACT_PID.matcher(it); m.matches(); m.group(1).toInt() }
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

class JavaRunExecCallback(private val exitValue: AtomicInteger, private val output: BufferInputStream,
                          private val error: BufferInputStream) : ResultCallback<Frame> {

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
        exitValue.set(0)
    }

    @Throws(IOException::class)
    override fun close() {
        println("close")
    }
}