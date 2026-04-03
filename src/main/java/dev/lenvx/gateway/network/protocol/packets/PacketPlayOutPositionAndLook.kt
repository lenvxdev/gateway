package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

class PacketPlayOutPositionAndLook(
	val x: Double,
	val y: Double,
	val z: Double,
	val yaw: Float,
	val pitch: Float,
	val teleportId: Int,
	vararg relatives: Relative
) : PacketOut() {

	enum class Relative(private val bit: Int) {
		X(0), Y(1), Z(2), Y_ROT(3), X_ROT(4),
		DELTA_X(5), DELTA_Y(6), DELTA_Z(7), ROTATE_DELTA(8);

		val mask: Int get() = 1 shl bit

		companion object {
			val ALL = values().toSet()
			val ROTATION = setOf(X_ROT, Y_ROT)
			val DELTA = setOf(DELTA_X, DELTA_Y, DELTA_Z, ROTATE_DELTA)

			fun union(vararg sets: Set<Relative>): Set<Relative> {
				return sets.flatMap { it }.toSet()
			}

			fun unpack(i: Int): Set<Relative> {
				val set = EnumSet.noneOf(Relative::class.java)
				for (relative in values()) {
					if ((i and relative.mask) == relative.mask) {
						set.add(relative)
					}
				}
				return set
			}

			fun pack(set: Set<Relative>): Int {
				var i = 0
				for (relative in set) {
					i = i or relative.mask
				}
				return i
			}
		}
	}

	val relatives: Set<Relative> = relatives.toSet()

	@Throws(IOException::class)
	override fun serializePacket(): ByteArray {
		val buffer = ByteArrayOutputStream()
		val output = DataOutputStream(buffer)
		
		DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
		DataTypeIO.writeVarInt(output, teleportId)
		output.writeDouble(x)
		output.writeDouble(y)
		output.writeDouble(z)
		output.writeDouble(0.0)
		output.writeDouble(0.0)
		output.writeDouble(0.0)
		output.writeFloat(yaw)
		output.writeFloat(pitch)
		output.writeInt(Relative.pack(relatives))
		
		return buffer.toByteArray()
	}

}

