package com.github.derteufelqwe.intellijjavaindockerplugin.utiliity

import com.github.fracpete.processoutput4j.core.StreamingProcessOutputType
import com.github.fracpete.processoutput4j.core.StreamingProcessOwner
import com.github.fracpete.rsync4j.RSync
import com.intellij.openapi.progress.ProgressIndicator


/**
 * Wrapper for RSync that removes the rsh parameter
 */
class WRSync : RSync() {

    override fun options(): MutableList<String> {
        if (this.getRsh().isEmpty()) {
            return super.options()
                .filter { o -> !o.startsWith("--rsh") } as MutableList<String>
        }

        return super.options()
    }
}


/**
 * Analyzes the output just to detect errors
 */
class ErrorDetectingProcessOwner : StreamingProcessOwner {

    var error = false
    val sb = StringBuilder()

    override fun getOutputType(): StreamingProcessOutputType {
        return StreamingProcessOutputType.STDERR
    }

    override fun processOutput(line: String?, stdout: Boolean) {
        error = true
        sb.append(line)
    }
}

/**
 * Just console print the RSync result
 */
class PrintProcessingOwner : StreamingProcessOwner {
    override fun getOutputType(): StreamingProcessOutputType {
        return StreamingProcessOutputType.BOTH
    }

    override fun processOutput(line: String?, stdout: Boolean) {
        println("[RSync] $line")
    }
}

/**
 * Used to process the file upload output
 */
class UploadProcessingOwner(private val indicator: ProgressIndicator) : StreamingProcessOwner {

    private val RE_START = Regex("^sending incremental file list\$")
    private val RE_END = Regex("^sent [0-9,]+ bytes.+\$")

    private var started = false


    override fun getOutputType(): StreamingProcessOutputType {
        return StreamingProcessOutputType.BOTH
    }

    override fun processOutput(line: String?, stdout: Boolean) {
        if (line == null) {
            return
        }

        if (!started) {
            if (RE_START.matches(line)) {
                started = true
            }

        } else {
            if (RE_END.matches(line)) {
                started = false
            }
        }

        if (started) {
            indicator.text2 = line
        }
    }
}
