package dev.lenvx.gateway.network.protocol.packets

import java.io.IOException

abstract class PacketOut : Packet() {

    @Throws(IOException::class)
    abstract fun serializePacket(): ByteArray

}

