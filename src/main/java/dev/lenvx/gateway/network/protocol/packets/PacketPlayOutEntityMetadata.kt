package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.entity.DataWatcher.WatchableObject
import dev.lenvx.gateway.entity.DataWatcher.WatchableObjectType
import dev.lenvx.gateway.entity.Entity
import dev.lenvx.gateway.entity.Pose
import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import dev.lenvx.gateway.utils.Rotation3f
import dev.lenvx.gateway.world.BlockPosition
import net.kyori.adventure.text.Component
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.Field
import java.nio.charset.StandardCharsets
import java.util.*

class PacketPlayOutEntityMetadata @JvmOverloads constructor(
	val entity: Entity,
	val allFields: Boolean = true,
	vararg val fields: Field
) : PacketOut() {

	companion object {
		const val END_OF_METADATA = 0xff
	}

	@Throws(IOException::class)
	override fun serializePacket(): ByteArray {
		val buffer = ByteArrayOutputStream()
		val output = DataOutputStream(buffer)
		
		DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
		DataTypeIO.writeVarInt(output, entity.entityId)
		
		val entries = entity.getDataWatcher()?.watchableObjects ?: return byteArrayOf()
		val watches: MutableSet<WatchableObject> = if (allFields) {
			HashSet(entries.values)
		} else {
			val set = HashSet<WatchableObject>()
			for (field in fields) {
				entries[field]?.let { set.add(it) }
			}
			set
		}
		
		val bitmasks = mutableMapOf<Int, Int>()
		val iterator = watches.iterator()
		while (iterator.hasNext()) {
			val watch = iterator.next()
			if (watch.isBitmask) {
				iterator.remove()
				var bitmask = bitmasks.getOrDefault(watch.index, 0)
				if (watch.value as Boolean) {
					bitmask = bitmask or watch.bitmask
				} else {
					bitmask = bitmask and watch.bitmask.inv()
				}
				bitmasks[watch.index] = bitmask
			}
		}
		for ((index, mask) in bitmasks) {
			watches.add(WatchableObject(mask.toByte(), index, WatchableObjectType.BYTE))
		}
		
		for (watch in watches) {
			output.writeByte(watch.index)
			if (watch.isOptional) {
				DataTypeIO.writeVarInt(output, watch.type.optionalTypeId)
				output.writeBoolean(watch.value != null)
			} else {
				DataTypeIO.writeVarInt(output, watch.type.typeId)
			}
			
			if (!watch.isOptional || watch.value != null) {
				when (watch.type) {
					WatchableObjectType.POSITION -> DataTypeIO.writeBlockPosition(output, watch.value as BlockPosition)
					WatchableObjectType.BOOLEAN -> output.writeBoolean(watch.value as Boolean)
					WatchableObjectType.BYTE -> output.writeByte((watch.value as Byte).toInt())
					WatchableObjectType.CHAT -> DataTypeIO.writeComponent(output, watch.value as Component)
					WatchableObjectType.FLOAT -> output.writeFloat(watch.value as Float)
					WatchableObjectType.POSE -> DataTypeIO.writeVarInt(output, (watch.value as Pose).id)
					WatchableObjectType.ROTATION -> {
						val rotation = watch.value as Rotation3f
						output.writeFloat(rotation.x.toFloat())
						output.writeFloat(rotation.y.toFloat())
						output.writeFloat(rotation.z.toFloat())
					}
					WatchableObjectType.STRING -> DataTypeIO.writeString(output, watch.value.toString(), StandardCharsets.UTF_8)
					WatchableObjectType.UUID -> DataTypeIO.writeUUID(output, watch.value as UUID)
					WatchableObjectType.VARINT -> DataTypeIO.writeVarInt(output, watch.value as Int)
					else -> {}
				}
			}
		}
		output.writeByte(END_OF_METADATA)
		
		return buffer.toByteArray()
	}

}

