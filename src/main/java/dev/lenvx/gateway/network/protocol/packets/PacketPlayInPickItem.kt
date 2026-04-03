package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException

class PacketPlayInPickItem(
    val slot: Int
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(DataTypeIO.readVarInt(input))
}
