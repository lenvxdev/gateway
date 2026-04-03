package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.UUID

class PacketLoginOutLoginSuccess(
    val uuid: UUID,
    val username: String
) : PacketOut() {

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
        DataTypeIO.writeUUID(output, uuid)
        DataTypeIO.writeString(output, username, StandardCharsets.UTF_8)
        DataTypeIO.writeVarInt(output, 0)
        return buffer.toByteArray()
    }
}
