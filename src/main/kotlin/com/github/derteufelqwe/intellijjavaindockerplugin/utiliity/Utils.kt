package com.github.derteufelqwe.intellijjavaindockerplugin.utiliity

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfiguration
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfigurationOptions
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint


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

    @JvmStatic
    fun showBalloonPopup(project: Project, msg: String, timeout: Long = 4000, type: MessageType = MessageType.WARNING) {
        val statusBar = WindowManager.getInstance().getStatusBar(project);

        JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(msg, type, null)
            .setFadeoutTime(timeout)
            .createBalloon()
            .show(RelativePoint.getCenterOf(statusBar.component), Balloon.Position.above)
    }

    @JvmStatic
    fun stopContainer(docker: DockerClient, options: JDRunConfigurationOptions) {
        if (options.reuseContainer) {
            return
        }

        try {
            docker.stopContainerCmd(options.hiddenContainerId!!).exec()
        } catch (_: NotFoundException) {
        }

        if (options.removeContainerOnStop) {
            try {
                docker.removeContainerCmd(options.hiddenContainerId!!).exec()
            } catch (_: NotFoundException) {
            }
        }

    }

}