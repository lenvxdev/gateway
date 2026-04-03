package dev.lenvx.gateway.bossbar

import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key

@Suppress("DeprecatedIsStillUsed")
@Deprecated("")
class Unsafe internal constructor(private val instance: KeyedBossBar) {

    @Deprecated("")
    fun a(): KeyedBossBar.GatewayBossBarHandler {
        return instance.listener
    }

    @Deprecated("")
    fun b() {
        instance.valid.set(false)
    }

    companion object {
        @JvmStatic
        @Deprecated("")
        fun a(key: Key, properties: BossBar): KeyedBossBar {
            return KeyedBossBar(key, properties)
        }
    }
}


