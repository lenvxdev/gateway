package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.location.GlobalPos
import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import net.kyori.adventure.key.Key
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class PacketPlayOutSpawnPosition(
    val position: GlobalPos,
    val yaw: Float,
    val pitch: Float
) : PacketOut() {

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
        DataTypeIO.writeString(output, Key.key(position.world.name).toString(), StandardCharsets.UTF_8)
        DataTypeIO.writeBlockPosition(output, position.pos)
        output.writeFloat(yaw)
        output.writeFloat(pitch)
        return buffer.toByteArray()
    }
}
