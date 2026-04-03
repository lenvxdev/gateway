package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.utils.DataTypeIO
import dev.lenvx.gateway.utils.LastSeenMessages
import dev.lenvx.gateway.utils.MessageSignature
import java.io.DataInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Instant

class PacketPlayInChat(
    val message: String,
    val time: Instant,
    val salt: Long,
    val signature: MessageSignature?,
    val lastSeenMessages: LastSeenMessages.b
) : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(
        DataTypeIO.readString(input, StandardCharsets.UTF_8),
        Instant.ofEpochMilli(input.readLong()),
        input.readLong(),
        if (input.readBoolean()) MessageSignature.read(input) else null,
        LastSeenMessages.b(input)
    )
}
