package dev.lenvx.gateway.network.protocol.packets

import java.io.DataInputStream
import java.io.IOException

class ServerboundFinishConfigurationPacket() : PacketIn() {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this()
}
