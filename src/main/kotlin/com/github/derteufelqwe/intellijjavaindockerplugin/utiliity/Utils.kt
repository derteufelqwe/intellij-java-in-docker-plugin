package com.github.derteufelqwe.intellijjavaindockerplugin.utiliity

import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfiguration
import com.github.derteufelqwe.intellijjavaindockerplugin.configs.JDRunConfigurationOptions
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.api.exception.NotFoundException
import com.github.dockerjava.api.exception.NotModifiedException
import com.github.dockerjava.api.model.*
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import java.util.regex.Pattern
import kotlin.jvm.Throws


object Utils {

    private val RE_PORT_CONFIG = Pattern.compile("^([0-9]+)(:([0-9]+))?(\\/([a-z]+))?\$")
    private val RE_VOLUME_CONFIG = Pattern.compile("^([\\w\\/\"' ]+)(:([\\w\\/\"' ]+))(:([a-z]+))?\$")

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
        if (options.useExistingContainer || options.reuseContainer || options.hiddenContainerId == null) {
            return
        }

        try {
            docker.stopContainerCmd(options.hiddenContainerId!!).exec()
        } catch (_: NotFoundException) {    // Container not found
            return
        } catch (_: NotModifiedException) { // Container already stopped
        }

        if (options.removeContainerOnStop) {
            try {
                docker.removeContainerCmd(options.hiddenContainerId!!).exec()
            } catch (_: NotFoundException) {
            }
        }

    }

    @JvmStatic
    @Throws(RuntimeConfigurationError::class)
    fun parseExposedPorts(data: String?) : List<PortInfo> {
        val splits = data?.split(" ") ?: listOf()
        val res = mutableListOf<PortInfo>()

        for (split in splits) {
            val m = RE_PORT_CONFIG.matcher(split);
            if (!m.matches()) {
                throw RuntimeConfigurationError("Invalid port config")
            }

            val portInfo = PortInfo(
                    if (m.group(1) == null) -1 else m.group(1).toInt(),
                    m.group(3),
                    if (m.group(5) == null) "" else m.group(5).trim().lowercase()
                )
            portInfo.validate()
            res.add(portInfo)
        }

        return res
    }

    @JvmStatic
    @Throws(RuntimeConfigurationError::class)
    fun parseAdditionalVolumes(data: String?) : List<VolumeInfo> {
        val splits = data?.splitWithQuotes() ?: listOf()
        val res = mutableListOf<VolumeInfo>()

        for (split in splits) {
            val m = RE_VOLUME_CONFIG.matcher(split)
            if (!m.matches()) {
                throw RuntimeConfigurationError("Invalid volume config.")
            }

            val volumeInfo = VolumeInfo(m.group(1), m.group(3), if (m.group(5) == null) null else m.group(5).trim().lowercase())
            volumeInfo.validate()
            res.add(volumeInfo)
        }

        return res
    }

    @JvmStatic
    fun createDockerConnection(url: String): DockerClient {
        val config = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost(url)
            .withDockerTlsVerify(false)
            .build()

        val client = OkDockerHttpClient.Builder()
            .dockerHost(config.dockerHost)
            .sslConfig(config.sslConfig)
            .connectTimeout(30)
            .build()

        return DockerClientImpl.getInstance(config, client)
    }

}

/**
 * Holds the exposed ports, specified by the user
 */
class PortInfo(val source: Int, val target: String?, val protocol: String) {

    @Throws(RuntimeConfigurationError::class)
    fun validate() {
        if (source < 0) {
            throw RuntimeConfigurationError("Source port can't be empty")
        }

        if (protocol !in listOf("", "tcp", "udp", "sctp")) {
            throw RuntimeConfigurationError("Protocol must be tcp, udp or sctp")
        }
    }

    private fun getProtocol() : InternetProtocol {
        return when (protocol) {
            "tcp" -> InternetProtocol.TCP
            "udp" -> InternetProtocol.UDP
            "sctp" -> InternetProtocol.SCTP
            else -> InternetProtocol.DEFAULT
        }
    }

    fun getExposedPort() : ExposedPort{
        return ExposedPort(source, getProtocol())
    }

    fun getPortBinding() : PortBinding {
        return PortBinding(
            Ports.Binding("0.0.0.0", target),
            getExposedPort()
        )
    }

    override fun toString(): String {
        return "PortInfo($source:$target/${getProtocol().name.lowercase()})"
    }
}

/**
 * Holds the mounted volumes, specified by the user
 */
class VolumeInfo(val source: String, val target: String, val mode: String?) {

    @Throws(RuntimeConfigurationError::class)
    fun validate() {
        if (mode != null && mode !in listOf("ro", "rw")) {
            throw RuntimeConfigurationError("Mount mode must be ro, rw or nothing")
        }
    }

    fun getMount() : Mount {
        return Mount()
            .withType(if (source.startsWith("/")) MountType.BIND else MountType.VOLUME)
            .withSource(source)
            .withTarget(target)
            .withReadOnly(mode == "ro")
    }

    override fun toString(): String {
        return "VolumeInfo($source:$target/$mode)"
    }
}