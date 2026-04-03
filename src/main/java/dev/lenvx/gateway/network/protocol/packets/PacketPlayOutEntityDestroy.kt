package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class PacketPlayOutEntityDestroy(vararg val entityIds: Int) : PacketOut() {

	@Throws(IOException::class)
	override fun serializePacket(): ByteArray {
		val buffer = ByteArrayOutputStream()
		val output = DataOutputStream(buffer)
		
		DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
		DataTypeIO.writeVarInt(output, entityIds.size)
		for (entityId in entityIds) {
			DataTypeIO.writeVarInt(output, entityId)
		}
		
		return buffer.toByteArray()
	}

}

