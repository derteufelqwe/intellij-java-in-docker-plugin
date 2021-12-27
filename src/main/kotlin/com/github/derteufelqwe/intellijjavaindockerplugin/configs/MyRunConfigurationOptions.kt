package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.intellij.execution.configurations.RunConfigurationOptions

class MyRunConfigurationOptions : RunConfigurationOptions() {

    val myScriptName = string("").provideDelegate(this, "scriptName")
    val myData = list<String>().provideDelegate(this, "data")


    fun getScriptName(): String? {
        return myScriptName.getValue(this)
    }

    fun setScriptName(name: String?) {
        myScriptName.setValue(this, name)
    }

    fun getData(): MutableList<String> {
        return myData.getValue(this)
    }

    fun setData(data: MutableList<String>) {
        myData.setValue(this, data)
    }

}