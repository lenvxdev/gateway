package dev.lenvx.gateway.utils

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.commands.CommandSender
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutDeclareCommands
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

object DeclareCommands {

    @JvmStatic
    @Throws(IOException::class)
    fun getDeclareCommandsPacket(sender: CommandSender): PacketPlayOutDeclareCommands {
        val commands = (Gateway.instance?.pluginManager?.getTabOptions(sender, emptyArray())
            ?: java.util.Collections.emptyList<String>()).toList()

        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)

        if (commands.isEmpty()) {
            DataTypeIO.writeVarInt(output, 1)

            output.writeByte(0)
            DataTypeIO.writeVarInt(output, 0)
            DataTypeIO.writeVarInt(output, 0)

            return PacketPlayOutDeclareCommands(buffer.toByteArray())
        }

        DataTypeIO.writeVarInt(output, commands.size * 2 + 1)

        output.writeByte(0)
        DataTypeIO.writeVarInt(output, commands.size)
        var i = 1
        while (i <= commands.size * 2) {
            DataTypeIO.writeVarInt(output, i++)
        }

        i = 1
        for (label in commands) {
            output.writeByte(1 or 0x04)
            DataTypeIO.writeVarInt(output, 1)
            DataTypeIO.writeVarInt(output, i + 1)
            DataTypeIO.writeString(output, label, StandardCharsets.UTF_8)
            i++

            output.writeByte(2 or 0x04 or 0x10)
            DataTypeIO.writeVarInt(output, 0)
            DataTypeIO.writeString(output, "arg", StandardCharsets.UTF_8)
            DataTypeIO.writeVarInt(output, 5)
            DataTypeIO.writeVarInt(output, 2)
            DataTypeIO.writeString(output, "minecraft:ask_server", StandardCharsets.UTF_8)
            i++
        }

        DataTypeIO.writeVarInt(output, 0)

        return PacketPlayOutDeclareCommands(buffer.toByteArray())
    }
}

