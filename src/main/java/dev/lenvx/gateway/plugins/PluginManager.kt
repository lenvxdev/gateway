package dev.lenvx.gateway.plugins

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.commands.CommandExecutor
import dev.lenvx.gateway.commands.CommandSender
import dev.lenvx.gateway.commands.DefaultCommands
import dev.lenvx.gateway.commands.TabCompletor
import dev.lenvx.gateway.file.FileConfiguration
import dev.lenvx.gateway.utils.SecurityValidation

import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.net.URLClassLoader
import java.util.Optional
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class PluginManager(private val defaultExecutor: DefaultCommands, private val pluginFolder: File) {

    val plugins: MutableMap<String, GatewayPlugin> = LinkedHashMap()
    private val executors: MutableList<Executor> = ArrayList()

    fun loadPlugins() {
        pluginFolder.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".jar")) {
                try {
                    ZipInputStream(FileInputStream(file)).use { zip ->
                        var gatewayYmlEntry: ZipEntry? = null
                        var legacyLimboYmlEntry: ZipEntry? = null
                        var pluginYmlEntry: ZipEntry? = null

                        while (true) {
                            val entry = zip.nextEntry ?: break
                            val name = entry.name
                            if (name.endsWith("gateway.yml")) {
                                gatewayYmlEntry = entry
                            } else if (name.endsWith("limbo.yml")) {
                                legacyLimboYmlEntry = entry
                            } else if (name.endsWith("plugin.yml")) {
                                pluginYmlEntry = entry
                            }
                        }

                        val chosenEntry = gatewayYmlEntry ?: legacyLimboYmlEntry ?: pluginYmlEntry

                        if (chosenEntry != null) {
                            ZipInputStream(FileInputStream(file)).use { processZip ->
                                while (true) {
                                    val currentEntry = processZip.nextEntry ?: break

                                    if (currentEntry.name == chosenEntry.name) {
                                        val pluginYaml = FileConfiguration(processZip)
                                        val mainClass = pluginYaml.get("main", String::class.java)!!
                                        val pluginName = pluginYaml.get("name", String::class.java)!!

                                        if (plugins.containsKey(pluginName)) {
                                            System.err.println("Ambiguous plugin name in " + file.name + " with the plugin \"" + plugins[pluginName]!!::class.java.name + "\"")
                                            break
                                        }

                                        val child = URLClassLoader(arrayOf(file.toURI().toURL()), Gateway.instance!!::class.java.classLoader)
                                        val clazz = Class.forName(mainClass, true, child)
                                        val pluginInstance = clazz.getDeclaredConstructor().newInstance()
                                        val plugin = when (pluginInstance) {
                                            is GatewayPlugin -> {
                                                pluginInstance.setInfo(pluginYaml, file)
                                                pluginInstance
                                            }
                                            is com.loohp.limbo.plugins.LimboPlugin -> {
                                                val adapter = LegacyLimboPluginAdapter(pluginInstance)
                                                adapter.setInfo(pluginYaml, file)
                                                adapter.syncLegacyInfo(com.loohp.limbo.file.FileConfiguration(pluginYaml), file)
                                                LegacyPluginBridge.register(pluginInstance, adapter)
                                                adapter
                                            }
                                            else -> throw IllegalStateException("Plugin main class must extend GatewayPlugin or com.loohp.limbo.plugins.LimboPlugin")
                                        }

                                        plugins[plugin.name] = plugin
                                        plugin.onLoad()
                                        Gateway.instance?.console?.sendMessage("Loading plugin " + file.name + " " + plugin.info.version + " by " + plugin.info.author)
                                        break
                                    }
                                }
                            }
                        } else {
                            System.err.println("Jar file " + file.name + " has no plugin.yml, gateway.yml, or legacy limbo.yml!")
                        }
                    }
                } catch (e: Exception) {
                    System.err.println("Unable to load plugin \"" + file.name + "\"")
                    e.printStackTrace()
                }
            }
        }
    }

    fun getPluginsList(): List<GatewayPlugin> {
        return ArrayList(plugins.values)
    }

    fun pluginsView(): List<GatewayPlugin> {
        return plugins.values.toList()
    }

    fun getPlugin(name: String): GatewayPlugin? {
        return plugins[name]
    }

    @JvmOverloads
    fun findPlugin(name: String, ignoreCase: Boolean = true): GatewayPlugin? {
        return if (!ignoreCase) {
            getPlugin(name)
        } else {
            plugins.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
        }
    }

    @JvmOverloads
    fun hasPlugin(name: String, ignoreCase: Boolean = true): Boolean {
        return findPlugin(name, ignoreCase) != null
    }

    @JvmOverloads
    fun requirePlugin(name: String, ignoreCase: Boolean = true): GatewayPlugin {
        return findPlugin(name, ignoreCase) ?: throw NoSuchElementException("Plugin not found: $name")
    }

    @Throws(Exception::class)
    fun fireExecutors(sender: CommandSender, args: Array<String>) {
        val properties = Gateway.instance?.serverProperties
        if (properties != null && !SecurityValidation.areCommandArgsSafe(args, properties.maxCommandArgs, properties.maxCommandLength)) {
            sender.sendMessage("Command rejected by security policy.")
            Gateway.instance?.runtimeMetrics?.incrementSecurityRejects()
            return
        }
        Gateway.instance?.console?.sendMessage(sender.name + " executed server command: /" + args.joinToString(" "))
        try {
            defaultExecutor.execute(sender, args)
        } catch (e: Exception) {
            System.err.println("Error while running default command \"" + args[0] + "\"")
            e.printStackTrace()
        }
        for (entry in executors) {
            try {
                entry.executor.execute(sender, args)
            } catch (e: Exception) {
                System.err.println("Error while passing command \"" + args[0] + "\" to the plugin \"" + entry.plugin.name + "\"")
                e.printStackTrace()
            }
        }
    }

    fun getTabOptions(sender: CommandSender, args: Array<String>): List<String> {
        val options: MutableList<String> = ArrayList()
        try {
            options.addAll(defaultExecutor.tabComplete(sender, args))
        } catch (e: Exception) {
            System.err.println("Error while getting default command tab completions")
            e.printStackTrace()
        }
        for (entry in executors) {
            if (entry.tab.isPresent) {
                try {
                    options.addAll(entry.tab.get().tabComplete(sender, args))
                } catch (e: Exception) {
                    System.err.println("Error while getting tab completions to the plugin \"" + entry.plugin.name + "\"")
                    e.printStackTrace()
                }
            }
        }
        return options
    }

    fun registerCommands(plugin: GatewayPlugin, executor: CommandExecutor) {
        executors.add(Executor(plugin, executor))
    }

    fun unregisterAllCommands(plugin: GatewayPlugin) {
        executors.removeIf { it.plugin == plugin }
    }

    fun getPluginFolderFile(): File {
        return File(pluginFolder.absolutePath)
    }

    fun pluginFolderFile(): File {
        return getPluginFolderFile()
    }

    class Executor(val plugin: GatewayPlugin, val executor: CommandExecutor) {
        val tab: Optional<TabCompletor> = if (executor is TabCompletor) {
            Optional.of(executor)
        } else {
            Optional.empty()
        }
    }
}



