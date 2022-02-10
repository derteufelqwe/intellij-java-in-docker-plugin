package com.github.derteufelqwe.intellijjavaindockerplugin

import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.WRSync
import com.github.fracpete.processoutput4j.core.StreamingProcessOutputType
import com.github.fracpete.processoutput4j.core.StreamingProcessOwner
import com.github.fracpete.processoutput4j.output.StreamingProcessOutput


fun main() {

    val arr = arrayOf(1, 2, 3)
    val list = listOf("A", "B", "C")
    val d = Data()

    println("Done")
}


class Data {

    fun arne() {

    }

    fun test() {
        println("Hello World")
    }

}


fun testRSync() {
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
}
