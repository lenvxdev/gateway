package com.loohp.limbo.plugins

import com.loohp.limbo.Limbo
import com.loohp.limbo.file.FileConfiguration
import dev.lenvx.gateway.Gateway
import java.io.File

open class LimboPlugin {

    var name: String = ""
        private set
    var dataFolder: File = File(".")
        private set
    lateinit var info: PluginInfo
        private set
    private var pluginJarFile: File? = null
        private set

    @Suppress("unused")
    protected fun setInfo(file: FileConfiguration, pluginJar: File) {
        initializeInfo(file, pluginJar)
    }

    fun initializeInfo(file: FileConfiguration, pluginJar: File) {
        this.info = PluginInfo(file)
        this.name = info.name
        this.dataFolder = File(Gateway.instance?.pluginFolder, name)
        this.pluginJarFile = pluginJar
    }

    @Suppress("unused")
    protected fun getPluginJar(): File? {
        return pluginJarFile?.absoluteFile
    }

    open fun onLoad() {}

    open fun onEnable() {}

    open fun onDisable() {}

    val gatewayOrNull: Gateway?
        get() = Gateway.instance

    val gateway: Gateway
        get() = Gateway.getInstance()

    val limboOrNull: Limbo?
        get() = Limbo.getInstance()

    val limbo: Limbo
        get() = Limbo.getInstance() ?: throw IllegalStateException("Gateway has not been initialized")

    fun getServer(): Limbo? {
        return Limbo.getInstance()
    }
}
