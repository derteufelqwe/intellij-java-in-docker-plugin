package com.github.derteufelqwe.intellijjavaindockerplugin

import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferInputStream
import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.BufferOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream


fun main() {
    val bs = BufferInputStream()
    bs.addData("h".encodeToByteArray())

    println(bs.read())
    println(bs.read())

}