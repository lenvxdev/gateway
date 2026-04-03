package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import net.kyori.adventure.key.Key
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class PacketLoginOutPluginMessaging(
    val messageId: Int,
    val channel: Key,
    val data: ByteArray?
) : PacketOut() {

    constructor(messageId: Int, channel: Key) : this(messageId, channel, null)

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
        DataTypeIO.writeVarInt(output, messageId)
        DataTypeIO.writeString(output, channel.toString(), StandardCharsets.UTF_8)
        if (data != null) {
            output.write(data)
        }
        return buffer.toByteArray()
    }
}
