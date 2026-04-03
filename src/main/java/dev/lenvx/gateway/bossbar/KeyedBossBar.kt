package dev.lenvx.gateway.bossbar

import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutBoss
import dev.lenvx.gateway.player.Player
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class KeyedBossBar internal constructor(val key: Key, val properties: BossBar) {

    val uniqueId: UUID = UUID.randomUUID()
    internal val players: MutableSet<Player> = ConcurrentHashMap.newKeySet()
    internal val listener: GatewayBossBarHandler = GatewayBossBarHandler(this)
    internal val valid: AtomicBoolean = AtomicBoolean(true)
    @Deprecated("")
    val unsafe: Unsafe = Unsafe(this)

    init {
        this.properties.addListener(listener)
    }

    val uuid: UUID
        get() = uniqueId

    fun getPlayers(): Set<Player> {
        return Collections.unmodifiableSet(players)
    }

    fun isValid(): Boolean {
        return valid.get()
    }

    fun showPlayer(player: Player): Boolean {
        val packetPlayOutBoss = PacketPlayOutBoss(this, PacketPlayOutBoss.BossBarAction.ADD)
        try {
            player.clientConnection.sendPacket(packetPlayOutBoss)
        } catch (ignore: IOException) {
        }
        return players.add(player)
    }

    fun hidePlayer(player: Player): Boolean {
        val packetPlayOutBoss = PacketPlayOutBoss(this, PacketPlayOutBoss.BossBarAction.REMOVE)
        try {
            player.clientConnection.sendPacket(packetPlayOutBoss)
        } catch (ignore: IOException) {
        }
        return players.remove(player)
    }

    class GatewayBossBarHandler internal constructor(private val parent: KeyedBossBar) : BossBar.Listener {

        override fun bossBarNameChanged(bar: BossBar, oldName: Component, newName: Component) {
            val packetPlayOutBoss = PacketPlayOutBoss(parent, PacketPlayOutBoss.BossBarAction.UPDATE_NAME)
            for (player in parent.getPlayers()) {
                try {
                    player.clientConnection.sendPacket(packetPlayOutBoss)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        override fun bossBarProgressChanged(bar: BossBar, oldProgress: Float, newProgress: Float) {
            val packetPlayOutBoss = PacketPlayOutBoss(parent, PacketPlayOutBoss.BossBarAction.UPDATE_PROGRESS)
            for (player in parent.getPlayers()) {
                try {
                    player.clientConnection.sendPacket(packetPlayOutBoss)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        override fun bossBarColorChanged(bar: BossBar, oldColor: BossBar.Color, newColor: BossBar.Color) {
            val packetPlayOutBoss = PacketPlayOutBoss(parent, PacketPlayOutBoss.BossBarAction.UPDATE_STYLE)
            for (player in parent.getPlayers()) {
                try {
                    player.clientConnection.sendPacket(packetPlayOutBoss)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        override fun bossBarOverlayChanged(bar: BossBar, oldOverlay: BossBar.Overlay, newOverlay: BossBar.Overlay) {
            val packetPlayOutBoss = PacketPlayOutBoss(parent, PacketPlayOutBoss.BossBarAction.UPDATE_STYLE)
            for (player in parent.getPlayers()) {
                try {
                    player.clientConnection.sendPacket(packetPlayOutBoss)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        override fun bossBarFlagsChanged(bar: BossBar, flagsAdded: Set<BossBar.Flag>, flagsRemoved: Set<BossBar.Flag>) {
            val packetPlayOutBoss = PacketPlayOutBoss(parent, PacketPlayOutBoss.BossBarAction.UPDATE_PROPERTIES)
            for (player in parent.getPlayers()) {
                try {
                    player.clientConnection.sendPacket(packetPlayOutBoss)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }
}


