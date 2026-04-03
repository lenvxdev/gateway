package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.inventory.EquipmentSlot
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException

class PacketPlayInBlockPlace(
    val hand: EquipmentSlot,
    val sequence: Int,
    val yRot: Float,
    val xRot: Float
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream, yRot: Float, xRot: Float) : this(
        EquipmentSlot.entries[DataTypeIO.readVarInt(input)],
        DataTypeIO.readVarInt(input),
        input.readFloat(),
        input.readFloat()
    )
}
