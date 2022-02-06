package com.github.derteufelqwe.intellijjavaindockerplugin

import com.github.fracpete.processoutput4j.core.StreamingProcessOutputType
import com.github.fracpete.processoutput4j.core.StreamingProcessOwner
import com.github.fracpete.processoutput4j.output.ConsoleOutputProcessOutput
import com.github.fracpete.processoutput4j.output.StreamingProcessOutput
import com.github.fracpete.rsync4j.RSync
import java.util.stream.Collectors


fun main() {

    val port = 49156

    val rsync = WRSync()
//        .recursive(true)
//        .times(true)
//        .dirs(true)
//        .verbose(true)
//        .update(true)
//        .source("C:\\Users\\Arne\\Documents\\Git\\intellij-java-in-docker-plugin\\build\\classes")
        .dryRun(true)
        .source("")
        .destination("rsync://ubuntu1:$port/files")

    val output = StreamingProcessOutput(object : StreamingProcessOwner {
        override fun getOutputType(): StreamingProcessOutputType {
            return StreamingProcessOutputType.BOTH
        }

        override fun processOutput(line: String?, stdout: Boolean) {
            println(line)
        }
    })
    output.monitor(rsync.builder())


    println("Done")
}


class WRSync : RSync() {

    override fun options(): MutableList<String> {
        if (this.getRsh().isEmpty()) {
            return super.options()
                .filter { o -> !o.startsWith("--rsh") } as MutableList<String>
        }

        return super.options()
    }
}