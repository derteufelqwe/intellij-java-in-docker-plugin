package com.github.derteufelqwe.intellijjavaindockerplugin.configs

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import javax.swing.Icon

class MyRunConfigType : ConfigurationType {

    companion object {
        val ID = "MyRunnConfig"
    }


    override fun getDisplayName(): String {
        return "Arne"
    }

    override fun getConfigurationTypeDescription(): String {
        return "Run Arnes run config"
    }

    override fun getIcon(): Icon {
        return AllIcons.General.Information
    }

    override fun getId(): String {
        return ID;
    }

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(MyConfigurationFactory(this))
    }
}