package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class ServerboundChatCommandPacket(
    val command: String
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(DataTypeIO.readString(input, StandardCharsets.UTF_8))
}
