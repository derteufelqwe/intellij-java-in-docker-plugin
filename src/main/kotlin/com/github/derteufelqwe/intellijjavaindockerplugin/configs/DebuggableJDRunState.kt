package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.github.derteufelqwe.intellijjavaindockerplugin.core.JDProcess
import com.github.dockerjava.api.DockerClient
import com.intellij.execution.*
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilder
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.BaseOSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.DebuggableRunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.psi.search.GlobalSearchScopes
import org.jetbrains.concurrency.Promise
import java.nio.charset.StandardCharsets

class DebuggableJDRunState(private val myEnvironment: ExecutionEnvironment?, private val docker: DockerClient) : DebuggableRunProfileState {

    private var myConsoleBuilder: TextConsoleBuilder? = null

    init {
        if (myEnvironment != null) {
            val project = myEnvironment.getProject()
            val searchScope = GlobalSearchScopes.executionScope(project, myEnvironment.getRunProfile())
            myConsoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope)
        }
    }

    fun getEnvironment(): ExecutionEnvironment? {
        return myEnvironment
    }

    fun getRunnerSettings(): RunnerSettings? {
        return myEnvironment!!.runnerSettings
    }

    fun getExecutionTarget(): ExecutionTarget {
        return myEnvironment!!.executionTarget
    }

    fun addConsoleFilters(vararg filters: Filter?) {
        myConsoleBuilder!!.filters(*filters)
    }

    @Throws(ExecutionException::class)
    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler = startProcess()
        val console = createConsole(executor)
        console?.attachToProcess(processHandler)

        return DefaultExecutionResult(console, processHandler, *createActions(console, processHandler, executor))
    }

    @Throws(ExecutionException::class)
    protected fun createConsole(executor: Executor): ConsoleView? {
        val builder = getConsoleBuilder()
        return builder?.console
    }


    @Throws(ExecutionException::class)
    protected fun startProcess(): ProcessHandler {
        val process = JDProcess(docker, myEnvironment!!.project)
        val processHandler: ProcessHandler = BaseOSProcessHandler(process, "command", StandardCharsets.UTF_8)

        ProcessTerminatedListener.attach(processHandler)
        processHandler.startNotify()

        return processHandler
    }


    override fun execute(debugPort: Int): Promise<ExecutionResult> {
        TODO("Not yet implemented")
    }

    protected fun createActions(console: ConsoleView?, processHandler: ProcessHandler?): Array<AnAction?> {
        return createActions(console, processHandler, null)
    }

    protected fun createActions(
        console: ConsoleView?,
        processHandler: ProcessHandler?,
        executor: Executor?
    ): Array<AnAction?> {
        return AnAction.EMPTY_ARRAY
    }

    fun getConsoleBuilder(): TextConsoleBuilder? {
        return myConsoleBuilder
    }

    fun setConsoleBuilder(consoleBuilder: TextConsoleBuilder?) {
        myConsoleBuilder = consoleBuilder
    }

}