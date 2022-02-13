package com.github.derteufelqwe.intellijjavaindockerplugin.settings

import com.github.derteufelqwe.intellijjavaindockerplugin.utiliity.Utils
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.DefaultDockerClientConfig
import com.github.dockerjava.core.DockerClientImpl
import com.github.dockerjava.okhttp.OkDockerHttpClient
import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import com.jetbrains.rd.util.first
import java.awt.Font
import java.awt.GridLayout
import java.io.UncheckedIOException
import java.lang.IllegalArgumentException
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.SwingConstants
import kotlin.math.roundToInt

class SettingsComponent {

    private var mainPanel: JPanel
    private var socketField = JBTextField()
    private var hostField = JBTextField()
    private var protocolField = JPanel(GridLayout(2, 1))
    private val protocolRadios = mutableMapOf<Protocol, JRadioButton>()
    private val checkBtn = JButton("Test connection")
    private val checkResLabel = JBLabel("")

    init {
        mainPanel = buildMainPanel()

        // Configure radio buttons
        val rb1 = JRadioButton("unix", true)
        val rb2 = JRadioButton("tcp")

        rb1.addActionListener {
            selectRadio(Protocol.UNIX)
        }
        rb2.addActionListener {
            selectRadio(Protocol.TCP)
        }

        protocolField.add(rb1)
        protocolField.add(rb2)

        protocolRadios.put(Protocol.UNIX, rb1)
        protocolRadios.put(Protocol.TCP, rb2)

        // Check button
        checkBtn.addActionListener {
            try {
                checkResLabel.text = "Connecting to docker engine ... "
                checkResLabel.icon = AnimatedIcon.Default()
                checkResLabel.horizontalAlignment = SwingConstants.LEFT

                val host = when(protocol) {
                    Protocol.UNIX -> socketText
                    Protocol.TCP -> hostText
                }
                val conn = Utils.createDockerConnection("${protocol.text}://$host")
                conn.pingCmd().exec()
                conn.close()
                checkResLabel.text = "Connection successful"
                checkResLabel.icon = AllIcons.General.InspectionsOK

            } catch (e: NoClassDefFoundError) {
                checkResLabel.icon = AllIcons.General.Error
                checkResLabel.text = "Unix sockets are not supported on windows"

            } catch (e: IllegalArgumentException) {
                checkResLabel.icon = AllIcons.General.Error

                if (e.message?.startsWith("unexpected port") == true) {
                    checkResLabel.text = "Port is missing"

                } else {
                    checkResLabel.text = "Connection error: ${e::class.simpleName}: ${e.message}"
                }

            } catch (e: UncheckedIOException) {
                checkResLabel.icon = AllIcons.General.Error

                if (e.message?.startsWith("Error while executing Request{method=GET, path=/_ping") == true) {
                    checkResLabel.text = "Connection failed"

                } else {
                    checkResLabel.text = "Connection error: ${e::class.simpleName}: ${e.message}"
                }

            } catch (e: Throwable) {
                checkResLabel.icon = AllIcons.General.Error
                checkResLabel.text = "Connection error: ${e::class.simpleName}: ${e.message}"
            }


        }

        checkResLabel.font = Font(checkResLabel.font.name, checkResLabel.font.style, (checkResLabel.font.size * 1.2).roundToInt())
    }

    /**
     * Selects the correct radio button based on Protocol
     */
    private fun selectRadio(protocol: Protocol) {
        protocolRadios.forEach { it.value.isSelected = false }

        val btn = protocolRadios[protocol] ?: return
        btn.isSelected = true
    }

    /**
     * Returns the selected protocol
     */
    private fun getRadio(): Protocol {
        return protocolRadios
            .filter { it.value.isSelected }
            .first()
            .key
    }


    private fun buildMainPanel(): JPanel {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Socket file"), socketField, 1, false)
            .addLabeledComponent(JBLabel("Docker host"), hostField, 1, false)
            .addLabeledComponent(JBLabel("Protocol"), protocolField, 1, false)
            .addVerticalGap(20)
            .addComponent(checkBtn, 1)
            .addVerticalGap(5)
            .addComponent(checkResLabel, 1)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getPanel(): JPanel {
        return this.mainPanel
    }

    // Declare settings field access methods here

    var hostText: String
        get() = hostField.text
        set(value) = hostField.setText(value)

    var socketText: String
        get() = socketField.text
        set(value) = socketField.setText(value)

    var protocol: Protocol
        get() = getRadio()
        set(value) = selectRadio(value)

}

/**
 * The selected docker api protocol
 */
enum class Protocol(val text: String) {
    UNIX("unix"),
    TCP("tcp")
    ;
}