package com.github.derteufelqwe.intellijjavaindockerplugin.configs;

import com.github.derteufelqwe.intellijjavaindockerplugin.MyBundle;
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferInputStream;
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferOutputStream;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;

import java.io.*;
import java.util.concurrent.TimeUnit;


public class MyProcess_orig extends Process {

    private int exitValue = -1;

    private BufferInputStream output = new BufferInputStream();
    private BufferInputStream error = new BufferInputStream();
    private BufferOutputStream input = new BufferOutputStream();

//    private InputStream output = new ByteArrayInputStream(new byte[1000]);
//    private InputStream error = new ByteArrayInputStream(new byte[1000]);
//    private OutputStream input = new ByteArrayOutputStream();


    private DockerClient docker;
    private Project project;


    public MyProcess_orig(DockerClient docker, Project project) {
        this.docker = docker;
        this.project = project;
        System.out.println("Creating process");

    }

    public void start() {

        try {
            TimeUnit.SECONDS.sleep(1);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Module module = ModuleManager.getInstance(project).getModules()[0];
        OrderEnumerator en = OrderEnumerator.orderEntries(project).recursively();

        ExecCreateCmdResponse resp = docker.execCreateCmd(MyBundle.CONTAINER_ID)
//                .withCmd("sh", "-c", "mkdir -p /javadeps && ls /")
//        java -classpath "/javadeps/*:/javadeps/classes" test.Main
                .withCmd("java", "-classpath", "/javadeps/*:/javadeps/classes", "test.Main")
                .withAttachStdin(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(true)
                .exec();


        docker.execStartCmd(resp.getId())
//                .withStdIn(new ByteArrayInputStream("hallo\nwelt\n".getBytes(StandardCharsets.UTF_8)))
                .withStdIn(input.getInput())
                .exec(new ResultCallback<Frame>() {

                    @Override
                    public void onStart(Closeable closeable) {
                        System.out.println("Start");
                    }

                    @Override
                    public void onNext(Frame object) {
                        switch (object.getStreamType()) {
                            case STDOUT:
                                output.addData(object.getPayload());
                                break;

                            case STDERR:
                                error.addData(object.getPayload());
                                break;

                            default:
                                throw new RuntimeException("Docker sent frame from " + object.getStreamType());
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        System.out.println("error");
                        throwable.printStackTrace();
                        exitValue = 1;
                    }

                    @Override
                    public void onComplete() {
                        System.out.println("complete");
                        exitValue = 0;
                    }

                    @Override
                    public void close() throws IOException {
                        System.out.println("close");
                    }
                });

    }

    @Override
    public OutputStream getOutputStream() {
        return input;
    }

    @Override
    public InputStream getInputStream() {
        return output;
    }

    @Override
    public InputStream getErrorStream() {
        return error;
    }

    @Override
    public int waitFor() throws InterruptedException {
        start();
        while (exitValue < 0) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        return exitValue;
    }

    @Override
    public int exitValue() {
        if (exitValue < 0) {
            throw new IllegalThreadStateException();
        }

        return exitValue;
    }

    @Override
    public void destroy() {
        output.close();
        error.close();
        input.close();
        exitValue = 0;
        System.out.println("destroy");
    }


}
