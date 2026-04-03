package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class PacketPlayOutStopSound(
    val sound: Key?,
    val source: Sound.Source?
) : PacketOut() {

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))

        if (source != null) {
            if (sound != null) {
                output.writeByte(3)
                DataTypeIO.writeVarInt(output, source.ordinal)
                DataTypeIO.writeString(output, sound.toString(), StandardCharsets.UTF_8)
            } else {
                output.writeByte(1)
                DataTypeIO.writeVarInt(output, source.ordinal)
            }
        } else if (sound != null) {
            output.writeByte(2)
            DataTypeIO.writeString(output, sound.toString(), StandardCharsets.UTF_8)
        } else {
            output.writeByte(0)
        }

        return buffer.toByteArray()
    }
}
