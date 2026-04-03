package com.loohp.limbo

import com.loohp.limbo.commands.CommandSender
import com.loohp.limbo.plugins.PluginManager
import dev.lenvx.gateway.Gateway
import java.io.File
import java.util.WeakHashMap

class Limbo private constructor(
    private val gateway: Gateway
) {

    val pluginManager: PluginManager
        get() = PluginManager.wrap(gateway.pluginManager)

    val server: Gateway
        get() = gateway

    val console: Console
        get() = Console.wrap(gateway.console)

    val pluginFolder: File
        get() = gateway.pluginFolder.absoluteFile

    fun dispatchCommand(sender: CommandSender, command: String) {
        gateway.dispatchCommand(sender as dev.lenvx.gateway.commands.CommandSender, command)
    }

    fun dispatchCommand(sender: CommandSender, vararg args: String) {
        gateway.dispatchCommand(sender as dev.lenvx.gateway.commands.CommandSender, *args)
    }

    companion object {
        private val wrappers = WeakHashMap<Gateway, Limbo>()

        @JvmStatic
        fun getInstance(): Limbo? {
            val current = Gateway.instance ?: return null
            return synchronized(wrappers) {
                wrappers.getOrPut(current) { Limbo(current) }
            }
        }
    }
}
