package com.github.derteufelqwe.intellijjavaindockerplugin.utiliity

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit


/**
 * Makes it possible to write to an InputStream
 */
class BufferInputStream : InputStream() {

    private val buffer = LinkedBlockingQueue<Byte>()
    private var open = true


    fun addData(data: ByteArray) {
        buffer.addAll(data.asIterable())
    }

    fun addData(data: String) {
        addData(data.encodeToByteArray())
    }

    override fun read(): Int {
        if (!open) {
            return -1
        }

        return buffer.poll(10, TimeUnit.MILLISECONDS)?.toInt() ?: -1
    }

    override fun available(): Int {
        return buffer.size
    }

    override fun close() {
        open = false
    }

}

