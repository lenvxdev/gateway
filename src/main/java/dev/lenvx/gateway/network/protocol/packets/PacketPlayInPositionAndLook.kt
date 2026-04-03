package dev.lenvx.gateway.network.protocol.packets

import java.io.DataInputStream
import java.io.IOException

class PacketPlayInPositionAndLook(
    val x: Double,
    val y: Double,
    val z: Double,
    val yaw: Float,
    val pitch: Float,
    val onGround: Boolean
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(
        input.readDouble(),
        input.readDouble(),
        input.readDouble(),
        input.readFloat(),
        input.readFloat(),
        input.readBoolean()
    )
}
