package com.github.derteufelqwe.intellijjavaindockerplugin.utiliity

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfiguration
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfigurationOptions
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications


object Utils {

    @JvmStatic
    fun getOptions(env: ExecutionEnvironment): JDRunConfigurationOptions {
        return (env.runProfile as JDRunConfiguration).getOptions2()
    }

    @JvmStatic
    fun showError(msg: String) {
        Notifications.Bus.notify(
            Notification("Java Docker",msg, NotificationType.ERROR)
        )
    }

}