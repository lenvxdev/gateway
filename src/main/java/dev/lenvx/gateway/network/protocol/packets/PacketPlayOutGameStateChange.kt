package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class PacketPlayOutGameStateChange(
    val event: GameStateChangeEvent,
    val value: Float
) : PacketOut() {

    enum class GameStateChangeEvent(private val id: Int) {
        NO_RESPAWN_BLOCK_AVAILABLE(0),
        START_RAINING(1),
        STOP_RAINING(2),
        CHANGE_GAME_MODE(3),
        WIN_GAME(4),
        DEMO_EVENT(5),
        ARROW_HIT_PLAYER(6),
        RAIN_LEVEL_CHANGE(7),
        THUNDER_LEVEL_CHANGE(8),
        PUFFER_FISH_STING(9),
        GUARDIAN_ELDER_EFFECT(10),
        IMMEDIATE_RESPAWN(11),
        LIMITED_CRAFTING(12),
        LEVEL_CHUNKS_LOAD_START(13);

        fun getId(): Int = id
    }

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
        output.writeByte(event.getId())
        output.writeFloat(value)
        return buffer.toByteArray()
    }
}
