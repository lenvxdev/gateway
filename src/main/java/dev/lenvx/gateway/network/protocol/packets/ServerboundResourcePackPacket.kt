package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException
import java.util.UUID

class ServerboundResourcePackPacket(
    val id: UUID,
    val action: Action
) : PacketIn() {

    enum class Action {
        SUCCESSFULLY_LOADED,
        DECLINED,
        FAILED_DOWNLOAD,
        ACCEPTED,
        DOWNLOADED,
        INVALID_URL,
        FAILED_RELOAD,
        DISCARDED;

        fun isTerminal(): Boolean {
            return this != ACCEPTED && this != DOWNLOADED
        }
    }

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(
        DataTypeIO.readUUID(input),
        Action.entries[DataTypeIO.readVarInt(input)]
    )
}
