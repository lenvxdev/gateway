package dev.lenvx.gateway.network.protocol.packets

import java.io.DataInputStream
import java.io.IOException

class PacketPlayInCloseWindow(
    val containerId: Int
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(input.readByte().toInt())
}
