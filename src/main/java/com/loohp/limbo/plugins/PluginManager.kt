package com.loohp.limbo.plugins

import com.loohp.limbo.commands.CommandExecutor
import com.loohp.limbo.commands.CommandSender
import com.loohp.limbo.commands.TabCompletor
import dev.lenvx.gateway.plugins.LegacyPluginBridge
import java.io.File
import java.util.WeakHashMap

class PluginManager private constructor(
    private val delegate: dev.lenvx.gateway.plugins.PluginManager
) {

    fun getPlugins(): List<LimboPlugin> {
        return getPluginsList()
    }

    fun registerCommands(plugin: LimboPlugin, executor: CommandExecutor) {
        val adapter = LegacyPluginBridge.adapterFor(plugin)
            ?: throw IllegalStateException("Legacy plugin is not loaded by Gateway: ${plugin.name}")
        delegate.registerCommands(adapter, adaptExecutor(executor))
    }

    fun unregisterAllCommands(plugin: LimboPlugin) {
        val adapter = LegacyPluginBridge.adapterFor(plugin) ?: return
        delegate.unregisterAllCommands(adapter)
    }

    @Suppress("unused")
    fun unregsiterAllCommands(plugin: LimboPlugin) {
        unregisterAllCommands(plugin)
    }

    fun getPluginFolderFile(): File {
        return delegate.getPluginFolderFile()
    }

    fun getPluginFolder(): File {
        return getPluginFolderFile()
    }

    fun getPlugin(name: String): LimboPlugin? {
        val gatewayPlugin = delegate.getPlugin(name) ?: return null
        return LegacyPluginBridge.legacyFor(gatewayPlugin)
    }

    fun getPluginsList(): List<LimboPlugin> {
        return delegate.getPluginsList().mapNotNull { LegacyPluginBridge.legacyFor(it) }
    }

    fun fireExecutors(sender: CommandSender, args: Array<String>) {
        val gatewaySender = sender as dev.lenvx.gateway.commands.CommandSender
        delegate.fireExecutors(gatewaySender, args)
    }

    fun getTabOptions(sender: CommandSender, args: Array<String>): List<String> {
        val gatewaySender = sender as dev.lenvx.gateway.commands.CommandSender
        return delegate.getTabOptions(gatewaySender, args)
    }

    companion object {
        private val wrappers = WeakHashMap<dev.lenvx.gateway.plugins.PluginManager, PluginManager>()

        @JvmStatic
        fun wrap(delegate: dev.lenvx.gateway.plugins.PluginManager): PluginManager {
            return synchronized(wrappers) {
                wrappers.getOrPut(delegate) { PluginManager(delegate) }
            }
        }
    }

    private fun adaptExecutor(executor: CommandExecutor): dev.lenvx.gateway.commands.CommandExecutor {
        return if (executor is TabCompletor) {
            object : dev.lenvx.gateway.commands.CommandExecutor, dev.lenvx.gateway.commands.TabCompletor {
                override fun execute(sender: dev.lenvx.gateway.commands.CommandSender, args: Array<String>) {
                    executor.execute(legacySender(sender), args)
                }

                override fun tabComplete(sender: dev.lenvx.gateway.commands.CommandSender, args: Array<String>): List<String> {
                    return executor.tabComplete(legacySender(sender), args)
                }
            }
        } else {
            object : dev.lenvx.gateway.commands.CommandExecutor {
                override fun execute(sender: dev.lenvx.gateway.commands.CommandSender, args: Array<String>) {
                    executor.execute(legacySender(sender), args)
                }
            }
        }
    }

    private fun legacySender(sender: dev.lenvx.gateway.commands.CommandSender): CommandSender {
        return object : CommandSender, dev.lenvx.gateway.commands.CommandSender by sender {}
    }
}
