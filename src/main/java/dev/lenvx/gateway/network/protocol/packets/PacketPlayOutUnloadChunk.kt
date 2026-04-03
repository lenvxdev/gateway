package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import dev.lenvx.gateway.world.ChunkPosition
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class PacketPlayOutUnloadChunk(val chunkPosition: ChunkPosition) : PacketOut() {

	@Throws(IOException::class)
	override fun serializePacket(): ByteArray {
		val buffer = ByteArrayOutputStream()
		val output = DataOutputStream(buffer)
		
		DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
		DataTypeIO.writeChunkPosition(output, chunkPosition)
		
		return buffer.toByteArray()
	}

}

