package dev.lenvx.gateway.consolegui

import dev.lenvx.gateway.Gateway

object ConsoleTextOutput {
    @JvmStatic
    fun appendText(string: String) {
        if (!Gateway.noGui) {
            GUI.textOutput.text = GUI.textOutput.text + string
            GUI.scrollPane.verticalScrollBar.value = GUI.scrollPane.verticalScrollBar.maximum
        }
    }

    @JvmStatic
    fun appendText(string: String, isWriteLine: Boolean) {
        if (isWriteLine) {
            appendText("$string\n")
        } else {
            appendText(string)
        }
    }
}

