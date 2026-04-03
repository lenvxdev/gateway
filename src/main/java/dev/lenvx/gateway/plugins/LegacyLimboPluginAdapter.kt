package dev.lenvx.gateway.plugins

import com.loohp.limbo.file.FileConfiguration
import com.loohp.limbo.plugins.LimboPlugin
import java.io.File

class LegacyLimboPluginAdapter(
    private val legacyPlugin: LimboPlugin
) : GatewayPlugin() {

    fun syncLegacyInfo(file: FileConfiguration, pluginJar: File) {
        legacyPlugin.initializeInfo(file, pluginJar)
    }

    override fun onLoad() {
        legacyPlugin.onLoad()
    }

    override fun onEnable() {
        legacyPlugin.onEnable()
    }

    override fun onDisable() {
        legacyPlugin.onDisable()
    }
}
