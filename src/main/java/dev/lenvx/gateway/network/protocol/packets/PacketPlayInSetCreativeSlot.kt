package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.inventory.ItemStack
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException

class PacketPlayInSetCreativeSlot(
    val slotNumber: Int,
    val itemStack: ItemStack
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(input.readShort().toInt(), DataTypeIO.readUntrustedItemStack(input))
}
