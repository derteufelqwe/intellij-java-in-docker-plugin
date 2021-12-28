package com.github.derteufelqwe.intellijjavaindockerplugin.configs;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.debugger.RemoteDebugConfiguration;

import java.net.InetSocketAddress;

public class MyRemoteDebugConfig extends RemoteDebugConfiguration {

    public MyRemoteDebugConfig(Project project, @NotNull ConfigurationFactory factory, String name, int defaultPort) {
        super(project, factory, name, defaultPort);
    }

    @Override
    public @NotNull XDebugProcess createDebugProcess(@NotNull InetSocketAddress socketAddress, @NotNull XDebugSession session, @Nullable ExecutionResult executionResult, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return new XDebugProcess(session) {

            @Override
            public @NotNull XDebuggerEditorsProvider getEditorsProvider() {
                return new XDebuggerEditorsProvider() {
                    @Override
                    public @NotNull FileType getFileType() {
                        return FileTypeManager.getInstance().getFileTypeByFileName("test.java");
                    }
                };
            }
        };
    }
}
