package dev.lenvx.gateway.network.protocol.packets

import java.io.DataInputStream
import java.io.IOException

class PacketPlayInKeepAlive(
    val payload: Long
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(input.readLong())
}
