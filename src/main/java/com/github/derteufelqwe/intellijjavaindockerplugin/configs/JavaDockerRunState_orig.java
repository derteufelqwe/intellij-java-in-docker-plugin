package com.github.derteufelqwe.intellijjavaindockerplugin.configs;

import com.github.derteufelqwe.intellijjavaindockerplugin.core.JDProcess;
import com.github.dockerjava.api.DockerClient;
import com.intellij.execution.*;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;

public class JavaDockerRunState_orig implements RunProfileState {

    private DockerClient docker;
    private TextConsoleBuilder myConsoleBuilder;

    private final ExecutionEnvironment myEnvironment;


    protected JavaDockerRunState_orig(ExecutionEnvironment environment, DockerClient docker) {
        myEnvironment = environment;
        if (myEnvironment != null) {
            final Project project = myEnvironment.getProject();
            final GlobalSearchScope searchScope = GlobalSearchScopes.executionScope(project, myEnvironment.getRunProfile());
            myConsoleBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project, searchScope);
        }
        this.docker = docker;
    }


    public ExecutionEnvironment getEnvironment() {
        return myEnvironment;
    }

    @Nullable
    public RunnerSettings getRunnerSettings() {
        return myEnvironment.getRunnerSettings();
    }

    @NotNull
    public ExecutionTarget getExecutionTarget() {
        return myEnvironment.getExecutionTarget();
    }

    public void addConsoleFilters(Filter... filters) {
        myConsoleBuilder.filters(filters);
    }

    @Override
    @NotNull
    public ExecutionResult execute(@NotNull final Executor executor, @NotNull final ProgramRunner<?> runner) throws ExecutionException {
        final ProcessHandler processHandler = startProcess();
        final ConsoleView console = createConsole(executor);
        if (console != null) {
            console.attachToProcess(processHandler);
        }
        return new DefaultExecutionResult(console, processHandler, createActions(console, processHandler, executor));
    }

    @Nullable
    protected ConsoleView createConsole(@NotNull final Executor executor) throws ExecutionException {
        TextConsoleBuilder builder = getConsoleBuilder();
        return builder != null ? builder.getConsole() : null;
    }

    /**
     * Starts the process.
     *
     * @return the handler for the running process
     * @throws ExecutionException if the execution failed.
     * @see GeneralCommandLine
     * @see com.intellij.execution.process.OSProcessHandler
     */
    @NotNull
    protected ProcessHandler startProcess() throws ExecutionException {

        JDProcess process = new JDProcess(docker, myEnvironment.getProject());
        ProcessHandler processHandler = new BaseOSProcessHandler(process, "command", StandardCharsets.UTF_8);

        ProcessTerminatedListener.attach(processHandler);


        processHandler.startNotify();

        return processHandler;
    }



    protected AnAction @NotNull [] createActions(final ConsoleView console, final ProcessHandler processHandler) {
        return createActions(console, processHandler, null);
    }

    protected AnAction @NotNull [] createActions(final ConsoleView console, final ProcessHandler processHandler, Executor executor) {
        return AnAction.EMPTY_ARRAY;
    }

    public TextConsoleBuilder getConsoleBuilder() {
        return myConsoleBuilder;
    }

    public void setConsoleBuilder(final TextConsoleBuilder consoleBuilder) {
        myConsoleBuilder = consoleBuilder;
    }
}
