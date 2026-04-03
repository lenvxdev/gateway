package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import net.kyori.adventure.text.Component
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Optional

class PacketPlayOutTabComplete(
    val id: Int,
    val start: Int,
    val length: Int,
    vararg matches: TabCompleteMatches
) : PacketOut() {

    val matches: Array<out TabCompleteMatches> = matches

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
        DataTypeIO.writeVarInt(output, id)
        DataTypeIO.writeVarInt(output, start)
        DataTypeIO.writeVarInt(output, length)
        DataTypeIO.writeVarInt(output, matches.size)

        for (match in matches) {
            DataTypeIO.writeString(output, match.match, StandardCharsets.UTF_8)
            if (match.tooltip.isPresent) {
                output.writeBoolean(true)
                DataTypeIO.writeComponent(output, match.tooltip.get())
            } else {
                output.writeBoolean(false)
            }
        }

        return buffer.toByteArray()
    }

    class TabCompleteMatches(
        val match: String,
        val tooltip: Optional<Component>
    ) {
        constructor(match: String) : this(match, Optional.empty())
        constructor(match: String, tooltip: Component?) : this(match, Optional.ofNullable(tooltip))
    }
}
