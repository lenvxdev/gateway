package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import net.kyori.adventure.text.Component
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.UUID

class ClientboundResourcePackPushPacket(
    val id: UUID,
    val url: String,
    val hash: String,
    val required: Boolean,
    val prompt: Component?
) : PacketOut() {

    init {
        require(hash.length <= MAX_HASH_LENGTH) { "Hash is too long (max $MAX_HASH_LENGTH, was ${hash.length})" }
    }

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
        DataTypeIO.writeUUID(output, id)
        DataTypeIO.writeString(output, url, StandardCharsets.UTF_8)
        DataTypeIO.writeString(output, hash, StandardCharsets.UTF_8)
        output.writeBoolean(required)
        if (prompt == null) {
            output.writeBoolean(false)
        } else {
            output.writeBoolean(true)
            DataTypeIO.writeComponent(output, prompt)
        }
        return buffer.toByteArray()
    }

    companion object {
        const val MAX_HASH_LENGTH: Int = 40
    }
}
