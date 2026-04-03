package dev.lenvx.gateway.utils

import com.google.common.io.ByteStreams
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutPluginMessaging
import net.kyori.adventure.key.Key
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.UUID

object BungeeLoginMessageUtils {

    @JvmField
    val BUNGEECORD_MAIN: String = Key.key("bungeecord", "main").toString()

    @JvmStatic
    @Throws(IOException::class)
    fun sendUUIDRequest(output: DataOutputStream) {
        val out = ByteStreams.newDataOutput()
        out.writeUTF("UUID")

        val packet = PacketPlayOutPluginMessaging(BUNGEECORD_MAIN, out.toByteArray())
        val packetByte = packet.serializePacket()
        DataTypeIO.writeVarInt(output, packetByte.size)
        output.write(packetByte)
    }

    @JvmStatic
    fun readUUIDResponse(data: ByteArray): UUID {
        val input = ByteStreams.newDataInput(data)
        val subchannel = input.readUTF()
        if (subchannel == "UUID") {
            return UUID.fromString(input.readUTF())
        }
        throw RuntimeException("BungeeCord message received is not a UUID")
    }

    @JvmStatic
    @Throws(IOException::class)
    fun sendIPRequest(output: DataOutputStream) {
        val out = ByteStreams.newDataOutput()
        out.writeUTF("IP")

        val packet = PacketPlayOutPluginMessaging(BUNGEECORD_MAIN, out.toByteArray())
        val packetByte = packet.serializePacket()
        DataTypeIO.writeVarInt(output, packetByte.size)
        output.write(packetByte)
    }

    @JvmStatic
    @Throws(UnknownHostException::class)
    fun readIPResponse(data: ByteArray): InetAddress {
        val input = ByteStreams.newDataInput(data)
        val subchannel = input.readUTF()
        if (subchannel == "IP") {
            val ip = input.readUTF()
            input.readInt()
            return InetAddress.getByName(ip)
        }
        throw RuntimeException("BungeeCord message received is not an IP")
    }
}
