package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.utils.DataTypeIO
import java.io.DataInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class PacketHandshakingIn(
    val protocolVersion: Int,
    val serverAddress: String,
    val serverPort: Int,
    val handshakeType: HandshakeType
) : PacketIn() {

    enum class HandshakeType(private val networkId: Int) {
        STATUS(1),
        LOGIN(2),
        TRANSFER(3);

        fun getNetworkId(): Int = networkId

        companion object {
            fun fromNetworkId(networkId: Int): HandshakeType {
                for (type in entries) {
                    if (type.getNetworkId() == networkId) {
                        return type
                    }
                }
                throw IllegalArgumentException("Unknown handshake type id: $networkId")
            }
        }
    }

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(
        DataTypeIO.readVarInt(input),
        DataTypeIO.readString(input, StandardCharsets.UTF_8),
        input.readShort().toInt() and 0xFFFF,
        HandshakeType.fromNetworkId(DataTypeIO.readVarInt(input))
    )
}
