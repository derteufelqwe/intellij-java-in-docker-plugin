package com.github.derteufelqwe.intellijjavaindockerplugin.core

import com.github.derteufelqwe.intellijjavaindockerplugin.MyBundle
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferInputStream
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferOutputStream
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import com.github.dockerjava.api.model.StreamType
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderEnumerator
import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit

class JDProcess(private val docker: DockerClient, private val project: Project) : Process() {

    private var exitValue = -1

    private val output = BufferInputStream()
    private val error = BufferInputStream()
    private val input = BufferOutputStream()

    init {
        println("Creating process")
    }


    fun start() {
        TimeUnit.SECONDS.sleep(1)

        val module = ModuleManager.getInstance(project).modules[0]
        val en = OrderEnumerator.orderEntries(project).recursively()

        val resp = docker.execCreateCmd(MyBundle.CONTAINER_ID)
//                .withCmd("sh", "-c", "mkdir -p /javadeps && ls /")
// java -classpath "/javadeps/*:/javadeps/classes" test.Main
                .withCmd("java", "-classpath", "/javadeps/*:/javadeps/classes", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "test.Main")
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)
                .exec()

        docker.execStartCmd(resp.id)
//                .withStdIn(new ByteArrayInputStream("hallo\nwelt\n".getBytes(StandardCharsets.UTF_8)))
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
                    println("error")
                    throwable.printStackTrace()
                    exitValue = 1
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
        output.close()
        error.close()
        input.close()
        exitValue = 0

        println("destroy")
    }

}