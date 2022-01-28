package com.github.derteufelqwe.intellijjavaindockerplugin


fun main() {

    try {
        throw NoSuchFieldException("fuck this")

    } catch (e: Exception) {
        println("error")
    }

}