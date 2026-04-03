package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.location.BlockFace
import dev.lenvx.gateway.utils.DataTypeIO
import dev.lenvx.gateway.world.BlockPosition
import java.io.DataInputStream
import java.io.IOException

class PacketPlayInBlockDig(
    val action: PlayerDigAction,
    val pos: BlockPosition,
    val direction: BlockFace,
    val sequence: Int
) : PacketIn() {

    enum class PlayerDigAction {
        START_DESTROY_BLOCK,
        ABORT_DESTROY_BLOCK,
        STOP_DESTROY_BLOCK,
        DROP_ALL_ITEMS,
        DROP_ITEM,
        RELEASE_USE_ITEM,
        SWAP_ITEM_WITH_OFFHAND
    }

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(
        PlayerDigAction.entries[DataTypeIO.readVarInt(input)],
        DataTypeIO.readBlockPosition(input),
        BlockFace.entries[input.readByte().toInt()],
        DataTypeIO.readVarInt(input)
    )
}
