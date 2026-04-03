package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class PacketConfigurationInPluginMessaging(
    val channel: String,
    data: ByteArray
) : PacketIn() {

    var data: ByteArray = data
        private set

    @Throws(IOException::class)
    constructor(input: DataInputStream, packetLength: Int, packetId: Int) : this(
        DataTypeIO.readString(input, StandardCharsets.UTF_8),
        ByteArray(0)
    ) {
        val dataLength = packetLength - DataTypeIO.getVarIntLength(packetId) - DataTypeIO.getStringLength(channel, StandardCharsets.UTF_8)
        val payload = ByteArray(dataLength)
        input.readFully(payload)
        data = payload
    }
}
