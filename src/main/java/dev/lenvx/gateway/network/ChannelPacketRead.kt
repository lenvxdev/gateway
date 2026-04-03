package dev.lenvx.gateway.network

import dev.lenvx.gateway.network.protocol.packets.PacketIn
import java.io.DataInput

class ChannelPacketRead(var size: Int, var packetId: Int, val dataInput: DataInput) {

    var readPacket: PacketIn? = null

    fun hasReadPacket(): Boolean {
        return readPacket != null
    }

}

