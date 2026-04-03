package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.registry.RegistryCustom
import dev.lenvx.gateway.utils.DataTypeIO
import net.kyori.adventure.key.Key
import net.querz.nbt.tag.CompoundTag
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class ClientboundRegistryDataPacket(
    val registry: RegistryCustom
) : PacketOut() {

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))

        DataTypeIO.writeString(output, registry.identifier.asString(), StandardCharsets.UTF_8)
        DataTypeIO.writeVarInt(output, registry.entries.size)
        for ((key, data) in registry.entries) {
            DataTypeIO.writeString(output, key.asString(), StandardCharsets.UTF_8)
            if (data == null) {
                output.writeBoolean(false)
            } else {
                output.writeBoolean(true)
                DataTypeIO.writeTag(output, data)
            }
        }

        return buffer.toByteArray()
    }
}
