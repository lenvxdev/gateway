package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.entity.EntityType
import dev.lenvx.gateway.location.Vector
import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

class PacketPlayOutSpawnEntity(
	val entityId: Int,
	val uuid: UUID,
	val type: EntityType,
	val x: Double,
	val y: Double,
	val z: Double,
	val pitch: Float,
	val yaw: Float,
	val headYaw: Float,
	val data: Int,
	movement: Vector
) : PacketOut() {

	val movement: Vector = movement.clone()

	@Throws(IOException::class)
	override fun serializePacket(): ByteArray {
		val buffer = ByteArrayOutputStream()
		val output = DataOutputStream(buffer)
		
		DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
		DataTypeIO.writeVarInt(output, entityId)
		DataTypeIO.writeUUID(output, uuid)
		DataTypeIO.writeVarInt(output, type.typeId.toInt())
		output.writeDouble(x)
		output.writeDouble(y)
		output.writeDouble(z)
		DataTypeIO.writeLpVec3(output, movement)
		output.writeByte((pitch * 256.0f / 360.0f).toInt())
		output.writeByte((yaw * 256.0f / 360.0f).toInt())
		output.writeByte((headYaw * 256.0f / 360.0f).toInt())
		DataTypeIO.writeVarInt(output, data)
		
		return buffer.toByteArray()
	}

}

