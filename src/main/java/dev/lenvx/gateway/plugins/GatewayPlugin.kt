package dev.lenvx.gateway.plugins

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.file.FileConfiguration

import java.io.File

open class GatewayPlugin {

    var name: String = ""
        private set
    var dataFolder: File = File(".")
        private set
    lateinit var info: PluginInfo
        private set
    var pluginJar: File? = null
        private set

    internal fun setInfo(file: FileConfiguration, pluginJar: File) {
        this.info = PluginInfo(file)
        this.name = info.name
        this.dataFolder = File(Gateway.instance?.pluginFolder, name)
        this.pluginJar = pluginJar
    }

    open fun onLoad() {}

    open fun onEnable() {}

    open fun onDisable() {}

    val gatewayOrNull: Gateway?
        get() = Gateway.instance

    val gateway: Gateway
        get() = Gateway.getInstance()

    fun getServer(): Gateway? {
        return Gateway.instance
    }

}



