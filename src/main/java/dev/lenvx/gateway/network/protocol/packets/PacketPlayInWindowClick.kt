package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.inventory.InventoryClickType
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException
import java.util.Collections
import java.util.HashSet

class PacketPlayInWindowClick(
    val containerId: Int,
    val stateId: Int,
    val slotNum: Int,
    val buttonNum: Int,
    val clickType: InventoryClickType,
    changedSlots: Set<Int>
) : PacketIn() {

    var changedSlots: Set<Int> = changedSlots
        private set

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(
        DataTypeIO.readVarInt(input),
        DataTypeIO.readVarInt(input),
        input.readShort().toInt(),
        input.readByte().toInt(),
        InventoryClickType.entries[DataTypeIO.readVarInt(input)],
        Collections.emptySet()
    ) {
        val slots = HashSet<Int>()
        val size = DataTypeIO.readVarInt(input)
        for (i in 0 until size) {
            val slot = input.readShort().toInt()
            DataTypeIO.consumeHashedStack(input)
            slots.add(slot)
        }
        changedSlots = Collections.unmodifiableSet(slots)
        DataTypeIO.consumeHashedStack(input)
    }
}
