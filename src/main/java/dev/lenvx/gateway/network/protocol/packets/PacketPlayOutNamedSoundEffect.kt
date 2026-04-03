package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.sounds.SoundEffect
import dev.lenvx.gateway.utils.DataTypeIO
import net.kyori.adventure.sound.Sound
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class PacketPlayOutNamedSoundEffect(
    val sound: SoundEffect,
    val source: Sound.Source,
    x: Double,
    y: Double,
    z: Double,
    val volume: Float,
    val pitch: Float,
    val seed: Long
) : PacketOut() {

    private val fixedX: Int = (x * 8.0).toInt()
    private val fixedY: Int = (y * 8.0).toInt()
    private val fixedZ: Int = (z * 8.0).toInt()

    val x: Double get() = fixedX.toDouble() / 8.0
    val y: Double get() = fixedY.toDouble() / 8.0
    val z: Double get() = fixedZ.toDouble() / 8.0

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))

        DataTypeIO.writeVarInt(output, 0)
        DataTypeIO.writeString(output, sound.sound.toString(), StandardCharsets.UTF_8)
        val fixedRange = sound.fixedRange()
        if (fixedRange.isPresent) {
            output.writeBoolean(true)
            output.writeFloat(fixedRange.get())
        } else {
            output.writeBoolean(false)
        }
        DataTypeIO.writeVarInt(output, source.ordinal)
        output.writeInt(fixedX)
        output.writeInt(fixedY)
        output.writeInt(fixedZ)
        output.writeFloat(volume)
        output.writeFloat(pitch)
        output.writeLong(seed)

        return buffer.toByteArray()
    }
}
