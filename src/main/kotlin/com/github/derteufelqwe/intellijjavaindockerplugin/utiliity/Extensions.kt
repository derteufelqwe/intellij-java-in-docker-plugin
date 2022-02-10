package com.github.derteufelqwe.intellijjavaindockerplugin.utiliity

import com.intellij.execution.configurations.RuntimeConfigurationError
import kotlin.jvm.Throws

/**
 * Splits a string respecting the quotes
 */
@Throws(RuntimeConfigurationError::class)
fun String.splitWithQuotes(): List<String> {
    val res = mutableListOf<String>()
    val sb = StringBuilder()
    var quoteOrder = 0  // 0 = nothing, 1 = ", 2 = ', 3 = "', 4 = '"

    for (chr in this) {
        if (chr == '\"') {
            when (quoteOrder) {
                0 -> quoteOrder = 1
                1 -> quoteOrder = 0
                2 -> quoteOrder = 4
                3 -> throw RuntimeConfigurationError("String $this is invalidly quoted. Close single quote first")
                4 -> quoteOrder = 2
            }

        } else if (chr == '\'') {
            when (quoteOrder) {
                0 -> quoteOrder = 2
                1 -> quoteOrder = 3
                2 -> quoteOrder = 0
                3 -> quoteOrder = 1
                4 -> throw RuntimeConfigurationError("String $this is invalidly quoted. Close double quote first")
            }
        }

        if (chr == ' ' && quoteOrder == 0) {
            if (sb.isNotBlank()) {
                res.add(sb.toString())
            }
            sb.clear()

        } else {
            sb.append(chr)
        }
    }

    if (sb.isNotBlank()) {
        res.add(sb.toString())
    }

    if (quoteOrder != 0) {
        throw RuntimeConfigurationError("String $this is invalidly quoted")
    }

    return res
}
