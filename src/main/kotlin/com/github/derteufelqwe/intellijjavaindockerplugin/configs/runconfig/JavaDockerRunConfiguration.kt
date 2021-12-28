package com.github.derteufelqwe.intellijjavaindockerplugin.configs.runconfig

import com.github.derteufelqwe.intellijjavaindockerplugin.MyBundle
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JavaDockerRunState
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.MyConfigurationFactory
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.MyForm
import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.command.ExecCreateCmdResponse
import com.github.dockerjava.api.model.Frame
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.ModuleManager
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

class JavaDockerRunConfiguration(project: Project, private val factory: MyConfigurationFactory, name: String) :
    RunConfigurationBase<JavaDockerRunConfiguration>(project, factory, name), ModuleRunProfile {
        

//    override fun getOptions(): MyRunConfigurationOptions {
//        return super.getOptions() as MyRunConfigurationOptions
//    }


    fun getScriptName(): String? {
        return "name"
//        return options.getScriptName()
    }

    fun setScriptName(scriptName: String?) {
//        options.setScriptName(scriptName)
    }

    fun setData(data: MutableList<String>) {
//        options.setData(data)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration?> {
        return MyForm()
    }


    /*
     * ModuleBasedConfiguration - for run config associated with module
     */


    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
//        StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
//        JBPopupFactory.getInstance()
//                .createHtmlTextBalloonBuilder("Dis is bad text", MessageType.INFO, null)
//                .setFadeoutTime(7500)
//                .createBalloon()
//                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.above);


        // java -classpath "/javadeps/*:/javadeps/classes" test.Main
        val module = ModuleManager.getInstance(project).modules[0]
        val en = OrderEnumerator.orderEntries(project).recursively()


        val existing = getAvailableFiles()

        ProgressManager.getInstance().runProcessWithProgressSynchronously({
            val indicator = ProgressManager.getInstance().progressIndicator
            indicator.isIndeterminate = false
            indicator.text2 = "Loading"
            val files = en.withoutSdk().classesRoots

            for (i in files.indices) {
                val file = files[i]
                val path = (file as? FsRoot)?.path?.substring(0, file.getPath().length - 2) ?: file.path

                if (file !is FsRoot || !existing.contains(file.getName())) {
                    indicator.text2 = file.name
                    factory.docker.copyArchiveToContainerCmd(MyBundle.CONTAINER_ID)
                        .withHostResource(path)
                        .withRemotePath("/javadeps")
                        .exec()
                }
                indicator.fraction = i / files.size.toDouble()
            }
        }, "Uploading dependencies and source code", true, project)


//        val state = object : JavaCommandLineState(environment) {
//
//            override fun createJavaParameters(): JavaParameters {
//                val params = JavaParameters()
//                params.mainClass = "TestingKt"
//                params.jdk = JavaParametersUtil.createProjectJdk(project, "D:\\Programme\\Java\\openjdk_11_28")
//
//                return params
//            }
//
//            override fun startProcess(): OSProcessHandler {
//                val process = JDProcess(factory.docker, environment.project)
//                val processHandler = JDOSProcessHandler(process, "command", StandardCharsets.UTF_8)
//
//                ProcessTerminatedListener.attach(processHandler)
//                processHandler.startNotify()
//
//                return processHandler
//            }
//
//        }
//
//        return state


        return JavaDockerRunState(environment, factory.docker)
    }

    private fun getAvailableFiles(): List<String> {
        val buffer = StringBuilder()
        val done = AtomicBoolean(false)
        val error = arrayOf<Throwable?>(null)

        val resp: ExecCreateCmdResponse = factory.docker.execCreateCmd(MyBundle.CONTAINER_ID)
            .withCmd("sh", "-c", "mkdir -p /javadeps && ls /javadeps")
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