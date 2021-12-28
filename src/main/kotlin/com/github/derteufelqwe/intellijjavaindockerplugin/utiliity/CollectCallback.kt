package com.github.derteufelqwe.intellijjavaindockerplugin.utiliity

import com.github.dockerjava.api.async.ResultCallback
import com.github.dockerjava.api.model.Frame
import java.io.Closeable
import java.util.concurrent.TimeUnit

class CollectCallback : ResultCallback<Frame> {

    private val sb = StringBuilder()
    val result: String
        get() = sb.toString()
    var done = false


    override fun onStart(closeable: Closeable?) {

    }

    override fun onNext(obj: Frame?) {
        obj?.let {
            sb.append(it.payload.decodeToString())
        }
    }

    override fun onError(throwable: Throwable?) {
        done = true
        throw RuntimeException("Collecting string failed.", throwable)
    }

    override fun onComplete() {
        done = true
    }

    override fun close() {
        done = true
    }


    fun await() {
        while (!done) {
            TimeUnit.MILLISECONDS.sleep(10)
        }
    }

}