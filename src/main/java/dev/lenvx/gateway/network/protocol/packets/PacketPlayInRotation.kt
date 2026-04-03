package dev.lenvx.gateway.network.protocol.packets

import java.io.DataInputStream
import java.io.IOException

class PacketPlayInRotation(
    val yaw: Float,
    val pitch: Float,
    val onGround: Boolean
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(input.readFloat(), input.readFloat(), input.readBoolean())
}
