package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException
import java.util.Optional

class PacketLoginInPluginMessaging(
    val messageId: Int,
    val successful: Boolean,
    data: ByteArray
) : PacketIn() {

    var data: Optional<ByteArray> = if (successful) Optional.of(data) else Optional.empty()
        private set

    @Throws(IOException::class)
    constructor(input: DataInputStream, packetLength: Int, packetId: Int) : this(
        DataTypeIO.readVarInt(input),
        input.readBoolean(),
        ByteArray(0)
    ) {
        if (successful) {
            val dataLength = packetLength - DataTypeIO.getVarIntLength(packetId) - DataTypeIO.getVarIntLength(messageId) - 1
            data = if (dataLength != 0) {
                val payload = ByteArray(dataLength)
                input.readFully(payload)
                Optional.of(payload)
            } else {
                Optional.of(ByteArray(0))
            }
        } else {
            data = Optional.empty()
        }
    }
}
