package com.github.derteufelqwe.intellijjavaindockerplugin.utiliity

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit


/**
 * Makes it possible to read from an OutputStream
 */
class BufferOutputStream : OutputStream() {

    private val buffer = LinkedBlockingQueue<Byte>()
    private var open = true
    val input = InputStreamFromOutput(this)


    override fun write(b: Int) {
        buffer.add(b.toByte())
    }

    fun available(): Int {
        return buffer.size
    }

    override fun close() {
        open = false
        input.close()
    }


    class InputStreamFromOutput(private val source: BufferOutputStream) : InputStream() {

        override fun read(): Int {
            if (!source.open) {
                return -1
            }

            var tmp: Byte? = null
            while (source.open && tmp == null) {
                tmp = source.buffer.poll(10, TimeUnit.MILLISECONDS)
            }

            return tmp?.toInt() ?: -1
        }

        override fun available(): Int {
            return source.available()
        }

    }

}

