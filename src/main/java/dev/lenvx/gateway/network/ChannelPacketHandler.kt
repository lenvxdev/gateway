package dev.lenvx.gateway.network

abstract class ChannelPacketHandler {

    open fun read(read: ChannelPacketRead): ChannelPacketRead? {
        return read
    }

    open fun write(write: ChannelPacketWrite): ChannelPacketWrite? {
        return write
    }

}

