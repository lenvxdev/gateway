package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import net.kyori.adventure.text.Component
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class ClientboundSetActionBarTextPacket(
    val actionBar: Component
) : PacketOut() {

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
        DataTypeIO.writeComponent(output, actionBar)
        return buffer.toByteArray()
    }
}
