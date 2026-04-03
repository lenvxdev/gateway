package dev.lenvx.gateway.consolegui

import dev.lenvx.gateway.Gateway
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.JTextPane
import javax.swing.UIManager
import javax.swing.UnsupportedLookAndFeelException

class GUI : JFrame() {

    companion object {
        @JvmStatic
        var contentPane: JPanel? = null
        @JvmStatic
        lateinit var commandInput: JTextField
        @JvmStatic
        lateinit var execCommand: JButton
        @JvmStatic
        lateinit var textOutput: JTextPane
        @JvmStatic
        lateinit var scrollPane: JScrollPane
        @JvmStatic
        lateinit var consoleLabel: JLabel
        @JvmStatic
        lateinit var clientLabel: JLabel
        @JvmStatic
        lateinit var clientText: JTextPane
        @JvmStatic
        lateinit var scrollPane_client: JScrollPane
        @JvmStatic
        lateinit var sysLabel: JLabel
        @JvmStatic
        lateinit var scrollPane_sys: JScrollPane
        @JvmStatic
        lateinit var sysText: JTextPane

        @JvmField
        var history: MutableList<String> = ArrayList()
        @JvmField
        var currenthistory = 0

        @JvmField
        var loadFinish = false

        @JvmStatic
        @Throws(UnsupportedLookAndFeelException::class, ClassNotFoundException::class, InstantiationException::class, IllegalAccessException::class)
        fun main() {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
            val frame = GUI()
            frame.isVisible = true

            val t1 = Thread { SystemInfo.printInfo() }
            t1.start()

            loadFinish = true
        }
    }

    init {
        title = "Gateway Minecraft Server"
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                if (Gateway.instance!!.isRunning()) {
                    Gateway.instance!!.stopServer()
                }
            }
        })
        setBounds(100, 100, 1198, 686)
        contentPane = JPanel()
        (contentPane as? javax.swing.JComponent)?.setBorder(javax.swing.border.EmptyBorder(5, 5, 5, 5))
        setContentPane(contentPane)
        val gbl_contentPane = GridBagLayout()
        gbl_contentPane.columnWidths = intArrayOf(243, 10, 36, 111, 0)
        gbl_contentPane.rowHeights = intArrayOf(0, 160, 0, 10, 33, 33, 0)
        gbl_contentPane.columnWidths = intArrayOf(243, 10, 36, 111, 0)
        gbl_contentPane.rowHeights = intArrayOf(0, 160, 0, 10, 33, 33, 0)
        gbl_contentPane.columnWeights = doubleArrayOf(0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE)
        gbl_contentPane.rowWeights = doubleArrayOf(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE)
        contentPane!!.layout = gbl_contentPane

        sysLabel = JLabel("System Information")
        sysLabel.font = Font("Arial", Font.BOLD, 11)
        val gbc_sysLabel = GridBagConstraints()
        gbc_sysLabel.fill = GridBagConstraints.BOTH
        gbc_sysLabel.insets = Insets(0, 0, 5, 5)
        gbc_sysLabel.gridx = 0
        gbc_sysLabel.gridy = 0
        contentPane!!.add(sysLabel, gbc_sysLabel)

        consoleLabel = JLabel("Console Output")
        consoleLabel.font = Font("Arial", Font.BOLD, 11)
        val gbc_consoleLabel = GridBagConstraints()
        gbc_consoleLabel.anchor = GridBagConstraints.WEST
        gbc_consoleLabel.insets = Insets(0, 0, 5, 5)
        gbc_consoleLabel.gridx = 2
        gbc_consoleLabel.gridy = 0
        contentPane!!.add(consoleLabel, gbc_consoleLabel)

        commandInput = JTextField()
        commandInput.toolTipText = "Input a command"
        commandInput.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == 10) {
                    val cmd = commandInput.text
                    if (commandInput.text != "") {
                        history.add(cmd)
                        currenthistory = history.size
                    }
                    Gateway.instance!!.dispatchCommand(Gateway.instance!!.console, cmd.trim { it <= ' ' }.replace(" +".toRegex(), " "))
                    commandInput.text = ""
                } else if (e.keyCode == 38) {
                    currenthistory--
                    if (currenthistory >= 0) {
                        commandInput.text = history[currenthistory]
                    } else {
                        currenthistory++
                    }
                } else if (e.keyCode == 40) {
                    currenthistory++
                    if (currenthistory < history.size) {
                        commandInput.text = history[currenthistory]
                    } else {
                        currenthistory--
                    }
                }
            }
        })

        scrollPane_sys = JScrollPane()
        val gbc_scrollPane_sys = GridBagConstraints()
        gbc_scrollPane_sys.insets = Insets(0, 0, 5, 5)
        gbc_scrollPane_sys.fill = GridBagConstraints.BOTH
        gbc_scrollPane_sys.gridx = 0
        gbc_scrollPane_sys.gridy = 1
        contentPane!!.add(scrollPane_sys, gbc_scrollPane_sys)

        sysText = JTextPane()
        sysText.font = Font("Consolas", Font.PLAIN, 12)
        sysText.isEditable = false
        scrollPane_sys.setViewportView(sysText)

        clientLabel = JLabel("Connected Clients")
        clientLabel.font = Font("Arial", Font.BOLD, 11)
        val gbc_clientLabel = GridBagConstraints()
        gbc_clientLabel.anchor = GridBagConstraints.WEST
        gbc_clientLabel.insets = Insets(0, 0, 5, 5)
        gbc_clientLabel.gridx = 0
        gbc_clientLabel.gridy = 3
        contentPane!!.add(clientLabel, gbc_clientLabel)

        scrollPane_client = JScrollPane()
        val gbc_scrollPane_client = GridBagConstraints()
        gbc_scrollPane_client.fill = GridBagConstraints.BOTH
        gbc_scrollPane_client.gridheight = 2
        gbc_scrollPane_client.insets = Insets(0, 0, 0, 5)
        gbc_scrollPane_client.gridx = 0
        gbc_scrollPane_client.gridy = 4
        contentPane!!.add(scrollPane_client, gbc_scrollPane_client)

        clientText = JTextPane()
        scrollPane_client.setViewportView(clientText)
        clientText.font = Font("Consolas", Font.PLAIN, 12)
        clientText.isEditable = false

        scrollPane = JScrollPane()
        val gbc_scrollPane = GridBagConstraints()
        gbc_scrollPane.gridheight = 4
        gbc_scrollPane.insets = Insets(0, 0, 5, 0)
        gbc_scrollPane.gridwidth = 2
        gbc_scrollPane.fill = GridBagConstraints.BOTH
        gbc_scrollPane.gridx = 2
        gbc_scrollPane.gridy = 1
        contentPane!!.add(scrollPane, gbc_scrollPane)

        textOutput = JTextPane()
        scrollPane.setViewportView(textOutput)
        textOutput.font = Font("Consolas", Font.PLAIN, 12)
        textOutput.isEditable = false
        commandInput.font = Font("Tahoma", Font.PLAIN, 19)
        val gbc_commandInput = GridBagConstraints()
        gbc_commandInput.insets = Insets(0, 0, 0, 5)
        gbc_commandInput.fill = GridBagConstraints.BOTH
        gbc_commandInput.gridx = 2
        gbc_commandInput.gridy = 5
        contentPane!!.add(commandInput, gbc_commandInput)
        commandInput.columns = 10

        execCommand = JButton("RUN")
        execCommand.toolTipText = "Execute a command"
        execCommand.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                val cmd = commandInput.text
                if (commandInput.text != "") {
                    history.add(cmd)
                    currenthistory = history.size
                }
                Gateway.instance!!.dispatchCommand(Gateway.instance!!.console, cmd.trim { it <= ' ' }.replace(" +".toRegex(), " "))
                commandInput.text = ""
            }
        })
        execCommand.font = Font("Tahoma", Font.PLAIN, 19)
        val gbc_execCommand = GridBagConstraints()
        gbc_execCommand.fill = GridBagConstraints.BOTH
        gbc_execCommand.gridx = 3
        gbc_execCommand.gridy = 5
        contentPane!!.add(execCommand, gbc_execCommand)
    }
}

