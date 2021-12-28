package com.github.derteufelqwe.intellijjavaindockerplugin.configs;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.OSProcessHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;

public class JDOSProcessHandler extends OSProcessHandler {

    public JDOSProcessHandler(@NotNull Process process, String commandLine, @Nullable Charset charset) {
        super(process, commandLine, charset);
    }

    @Override
    protected boolean shouldDestroyProcessRecursively() {
        return false;
    }


}
