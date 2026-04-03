package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class PacketPlayOutPlayerAbilities(
    val flySpeed: Float,
    val fieldOfField: Float,
    vararg flags: PlayerAbilityFlags
) : PacketOut() {

    val flags: Array<out PlayerAbilityFlags> = flags

    enum class PlayerAbilityFlags(private val bitvalue: Int) {
        INVULNERABLE(0x01),
        FLY(0x02),
        ALLOW_FLYING(0x04),
        CREATIVE(0x08);

        fun getValue(): Int = bitvalue
    }

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))

        var value: Byte = 0
        for (flag in flags) {
            value = (value.toInt() or flag.getValue()).toByte()
        }
        output.writeByte(value.toInt())
        output.writeFloat(flySpeed)
        output.writeFloat(fieldOfField)

        return buffer.toByteArray()
    }
}
