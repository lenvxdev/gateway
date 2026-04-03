package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.inventory.EquipmentSlot
import dev.lenvx.gateway.location.MovingObjectPositionBlock
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException

class PacketPlayInUseItem(
    val hand: EquipmentSlot,
    val blockHit: MovingObjectPositionBlock,
    val sequence: Int
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(
        EquipmentSlot.entries[DataTypeIO.readVarInt(input)],
        DataTypeIO.readBlockHitResult(input),
        DataTypeIO.readVarInt(input)
    )
}
