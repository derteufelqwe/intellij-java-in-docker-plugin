package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.core.JDProcess
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.*
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.util.CompressArchiveUtil
import com.github.fracpete.processoutput4j.output.StreamingProcessOutput
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.newvfs.impl.FsRoot
import com.intellij.util.io.inputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class JDRunState(env: ExecutionEnvironment, private val docker: DockerClient, private val options: JDRunConfigurationOptions) : CommandLineState(env) {

    override fun startProcess(): ProcessHandler {
        if (!setupRSync(options)) {
            throw RuntimeException("RSync upload failed")
        }

        if (!uploadDependencyFiles(options)) {
            throw RuntimeException("Dependency upload failed")
        }

        val process = JDProcess(docker, environment, createClasspath())
        val processHandler: ProcessHandler = JDProcessHandler(process, "command", StandardCharsets.UTF_8)

        ProcessTerminatedListener.attach(processHandler)
        processHandler.addProcessListener(ContainerStopListener(docker, options))
        processHandler.startNotify()

        return processHandler
    }


    private fun createClasspath(): List<String> {
        val module = ModuleManager.getInstance(environment.project).modules[0]
        val files = OrderEnumerator.orderEntries(environment.project).recursively().withoutSdk().classesRoots

        return files.map { (it as? FsRoot)?.path?.substring(0, it.getPath().length - 2) ?: it.path }
    }

    /**
     * Creates a tar of the rsync files
     */
    private fun tarRSyncFiles(): InputStream {
        val files = listOf("rrsync", "rsync", "rsync-ssl", "rsyncd.conf", "startRSync.sh")
        val cl = this::class.java.classLoader

        val tarFile = Files.createTempFile("javadocker", ".tar.gz")
        val tmpDir = Files.createTempDirectory("javadocker").toFile()
        tmpDir.mkdir()
        val rsyncDir = File(tmpDir, "rsync")
        rsyncDir.mkdir()

        val tmpFiles = mutableListOf<File>()

        for (file in files) {
            val tmpFile = File(rsyncDir, file)
            tmpFile.createNewFile()
            tmpFile.writeBytes(cl.getResourceAsStream("rsync/" + file).readAllBytes())
            tmpFiles.add(tmpFile)
        }

        CompressArchiveUtil.tar(rsyncDir.toPath(), tarFile, true, false)
        tmpFiles.forEach(File::delete)

        return tarFile.inputStream()
    }

    /**
     * Uploads rsync to the container
     */
    private fun setupRSync(options: JDRunConfigurationOptions): Boolean {
        var success = false

        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            try {
                val indicator = ProgressManager.getInstance().progressIndicator
                indicator.isIndeterminate = false
                indicator.fraction = 0.0
                indicator.text2 = "Checking for RSync"

                // Skip setup if RSync is already up and running
                if (verifyRSyncWorking()) {
                    success = true
                    return@runProcessWithProgressSynchronously
                }

                // Copy rsync
                indicator.fraction = 0.1
                indicator.text2 = "Uploading RSync"
                docker.copyArchiveToContainerCmd(options.hiddenContainerId!!)
                    .withTarInputStream(tarRSyncFiles())
                    .withRemotePath("/")
                    .exec()

                // Move config
                indicator.fraction = 0.9
                indicator.text2 = "Configuring RSync"
                val moveExec = docker.execCreateCmd(options.hiddenContainerId!!)
                    .withWorkingDir("/rsync")
                    .withCmd("mv", "rsyncd.conf", "/etc/rsyncd.conf")
                    .exec()

                val cb = docker.execStartCmd(moveExec.id)
                    .exec(CollectCallback())
                cb.await()

                if (cb.result != "") {
                    Utils.showError("Moving rsyncd.conf failed: " + cb.result)
                    return@runProcessWithProgressSynchronously
                }

                // Start rsync daemon
                val startRSync = docker.execCreateCmd(options.hiddenContainerId!!)
                    .withWorkingDir("/rsync")
                    .withCmd("sh", "startRSync.sh")
                    .exec()

                docker.execStartCmd(startRSync.id)
                    .exec(CollectCallback()).await()


                // Check if RSync is running
                indicator.fraction = 1.0
                indicator.text2 = "Verify that RSync is working"
               if (!verifyRSyncWorking()) {
                    Utils.showError("Failed to start RSync.")
                    return@runProcessWithProgressSynchronously
                }

                success = true

            } catch (e: Exception) {
                Utils.showError("Failed to upload / configure RSync")
                throw e
            }

        }, "Uploading and configuring RSync", true, environment.project)

        return success
    }

    /**
     * Checks if RSync is working
     */
    private fun verifyRSyncWorking(): Boolean {
        val rsync = WRSync()
            .dryRun(true)
            .source("")
            .destination("rsync://ubuntu1:${options.rsyncPort}/files")

        val owner = ErrorDetectingProcessOwner()
        val output = StreamingProcessOutput(owner)
        output.monitor(rsync.builder())

        return !owner.error
    }

    private fun uploadDependencyFiles(options: JDRunConfigurationOptions): Boolean {
        var success = false

        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            try {
                val indicator = ProgressManager.getInstance().progressIndicator
                indicator.isIndeterminate = false
                indicator.text2 = ""

                val tStart = System.currentTimeMillis()
                val paths = createClasspath()

                val rsync = WRSync()
                    .recursive(true)
                    .times(true)
                    .dirs(true)
                    .verbose(true)
                    .update(true)
                    .sources(paths)
                    .destination("rsync://ubuntu1:${options.rsyncPort}/files")

                val output = StreamingProcessOutput(UploadProcessingOwner(indicator))
                output.monitor(rsync.builder())


                println("Upload took: ${System.currentTimeMillis() - tStart}ms")
                success = true

            } catch (e: Exception) {
                Utils.showError("Dependency upload failed.")
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
    constructor(process: Process, commandLine: String?, charset: Charset) : super(process, commandLine, charset)

    override fun destroyProcessGracefully(): Boolean {
        // Without this the super method logs an error, which can't be prevented and causes the IDE to show an exception
        return false
    }
}


/**
 * Listens for process termination to kill the process if necessary
 */
class ContainerStopListener(private val docker: DockerClient, private val options: JDRunConfigurationOptions) : ProcessListener {

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