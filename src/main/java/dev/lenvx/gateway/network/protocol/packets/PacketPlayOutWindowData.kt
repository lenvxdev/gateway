package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class PacketPlayOutWindowData(
    val containerId: Int,
    val id: Int,
    val value: Int
) : PacketOut() {

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
        output.writeByte(containerId)
        output.writeShort(id)
        output.writeShort(value)
        return buffer.toByteArray()
    }
}
