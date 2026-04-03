package dev.lenvx.gateway.network.protocol.packets

import java.io.DataInputStream
import java.io.IOException

class PacketPlayInPosition(
    val x: Double,
    val y: Double,
    val z: Double,
    val onGround: Boolean
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(input.readDouble(), input.readDouble(), input.readDouble(), input.readBoolean())
}
