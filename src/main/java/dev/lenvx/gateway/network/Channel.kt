package dev.lenvx.gateway.network

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.network.protocol.packets.PacketIn
import dev.lenvx.gateway.network.protocol.packets.PacketOut
import dev.lenvx.gateway.utils.DataTypeIO
import dev.lenvx.gateway.utils.Pair
import net.kyori.adventure.key.Key
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

class Channel(private val client: ClientConnection, internal val input: DataInputStream, internal val output: DataOutputStream) : AutoCloseable {

    private val handlers: MutableList<Pair<Key, ChannelPacketHandler>> = CopyOnWriteArrayList()
    private val isValid = AtomicBoolean(true)

    private fun ensureOpen() {
        if (!isValid.get()) {
            close()
        }
    }

    fun addHandlerBefore(key: Key, handler: ChannelPacketHandler) {
        handlers.add(0, Pair(key, handler))
    }

    fun addHandlerAfter(key: Key, handler: ChannelPacketHandler) {
        handlers.add(Pair(key, handler))
    }

    fun removeHandler(key: Key) {
        handlers.removeIf { it.first == key }
    }

    @Throws(Exception::class)
    internal fun readPacket(): PacketIn? {
        return readPacket(-1)
    }

    @Throws(IOException::class)
    internal fun readPacket(initialSize: Int): PacketIn? {
        var size = initialSize
        var packet: PacketIn? = null
        do {
            ensureOpen()
            size = if (size < 0) DataTypeIO.readVarInt(input) else size
            val maxPacketSize = Gateway.instance?.serverProperties?.maxPacketSizeBytes ?: 262144
            if (size <= 0 || size > maxPacketSize) {
                throw IOException("Inbound packet frame size $size exceeds configured limit $maxPacketSize")
            }
            val packetId = DataTypeIO.readVarInt(input)
            var read: ChannelPacketRead? = ChannelPacketRead(size, packetId, input)
            for (pair in handlers) {
                read = read?.let { pair.second.read(it) }
                if (read == null) {
                    packet = null
                    break
                }
                packet = read.readPacket
            }
            size = -1
        } while (packet == null)
        return packet
    }

    @Throws(IOException::class)
    internal fun writePacket(packetOut: PacketOut): Boolean {
        var packet = packetOut
        if (client.clientState == ClientConnection.ClientState.DISCONNECTED) {
            return false
        }
        ensureOpen()
        if (packet.getPacketState() != client.clientState) {
            return false
        }
        var write: ChannelPacketWrite? = ChannelPacketWrite(packet)
        for (pair in handlers) {
            write = write?.let { pair.second.write(it) }
            if (write == null) {
                return false
            }
        }
        packet = write!!.packet
        val packetByte = packet.serializePacket()
        writePacketRaw(packetByte)
        return true
    }

    @Throws(IOException::class)
    fun writePacketRaw(packetByte: ByteArray) {
        DataTypeIO.writeVarInt(output, packetByte.size)
        output.write(packetByte)
        output.flush()
    }

    @Synchronized
    override fun close() {
        if (isValid.compareAndSet(true, false)) {
            try {
                input.close()
                output.close()
            } catch (ignore: Exception) {
            }
        }
    }
}


