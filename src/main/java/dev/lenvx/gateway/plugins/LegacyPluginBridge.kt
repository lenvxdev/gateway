package dev.lenvx.gateway.plugins

import com.loohp.limbo.plugins.LimboPlugin
import java.util.concurrent.ConcurrentHashMap

object LegacyPluginBridge {

    private val legacyToAdapter = ConcurrentHashMap<LimboPlugin, LegacyLimboPluginAdapter>()
    private val adapterToLegacy = ConcurrentHashMap<LegacyLimboPluginAdapter, LimboPlugin>()

    @JvmStatic
    fun register(legacyPlugin: LimboPlugin, adapter: LegacyLimboPluginAdapter) {
        legacyToAdapter[legacyPlugin] = adapter
        adapterToLegacy[adapter] = legacyPlugin
    }

    @JvmStatic
    fun adapterFor(legacyPlugin: LimboPlugin): LegacyLimboPluginAdapter? {
        return legacyToAdapter[legacyPlugin]
    }

    @JvmStatic
    fun legacyFor(adapter: GatewayPlugin): LimboPlugin? {
        return if (adapter is LegacyLimboPluginAdapter) adapterToLegacy[adapter] else null
    }
}
