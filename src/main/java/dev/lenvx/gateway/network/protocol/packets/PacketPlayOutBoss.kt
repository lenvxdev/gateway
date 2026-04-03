package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.bossbar.KeyedBossBar
import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import net.kyori.adventure.bossbar.BossBar
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class PacketPlayOutBoss(
    val bossBar: KeyedBossBar,
    val action: BossBarAction
) : PacketOut() {

    enum class BossBarAction {
        ADD,
        REMOVE,
        UPDATE_PROGRESS,
        UPDATE_NAME,
        UPDATE_STYLE,
        UPDATE_PROPERTIES
    }

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))

        DataTypeIO.writeUUID(output, bossBar.uniqueId)
        DataTypeIO.writeVarInt(output, action.ordinal)

        val properties = bossBar.properties
        when (action) {
            BossBarAction.ADD -> {
                DataTypeIO.writeComponent(output, properties.name())
                output.writeFloat(properties.progress())
                DataTypeIO.writeVarInt(output, properties.color().ordinal)
                DataTypeIO.writeVarInt(output, properties.overlay().ordinal)
                output.writeByte(encodeProperties(properties.hasFlag(BossBar.Flag.DARKEN_SCREEN), properties.hasFlag(BossBar.Flag.PLAY_BOSS_MUSIC), properties.hasFlag(BossBar.Flag.CREATE_WORLD_FOG)))
            }
            BossBarAction.REMOVE -> Unit
            BossBarAction.UPDATE_PROGRESS -> output.writeFloat(properties.progress())
            BossBarAction.UPDATE_NAME -> DataTypeIO.writeComponent(output, properties.name())
            BossBarAction.UPDATE_STYLE -> {
                DataTypeIO.writeVarInt(output, properties.color().ordinal)
                DataTypeIO.writeVarInt(output, properties.overlay().ordinal)
            }
            BossBarAction.UPDATE_PROPERTIES -> output.writeByte(encodeProperties(properties.hasFlag(BossBar.Flag.DARKEN_SCREEN), properties.hasFlag(BossBar.Flag.PLAY_BOSS_MUSIC), properties.hasFlag(BossBar.Flag.CREATE_WORLD_FOG)))
        }

        return buffer.toByteArray()
    }

    companion object {
        private fun encodeProperties(darkenScreen: Boolean, playMusic: Boolean, createWorldFog: Boolean): Int {
            var value = 0
            if (darkenScreen) value = value or 1
            if (playMusic) value = value or 2
            if (createWorldFog) value = value or 4
            return value
        }
    }
}
