package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.BuiltInRegistries
import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.BitsUtils
import dev.lenvx.gateway.utils.DataTypeIO
import dev.lenvx.gateway.world.Environment
import dev.lenvx.gateway.world.GeneratedBlockDataMappings
import net.kyori.adventure.key.Key
import net.querz.mca.Chunk
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.ceil

class ClientboundLevelChunkWithLightPacket(
	val chunkX: Int,
	val chunkZ: Int,
	val chunk: Chunk,
	val environment: Environment,
	val skylightArrays: List<Array<Byte>?>,
	val blocklightArrays: List<Array<Byte>?>
) : PacketOut() {

	private val skyLightBitMasks: LongArray
	private val blockLightBitMasks: LongArray
	private val skyLightBitMasksEmpty: LongArray
	private val blockLightBitMasksEmpty: LongArray

	init {
		val skyLightBitSet = BitSet()
		val skyLightBitSetInverse = BitSet()
		for (i in (skylightArrays.size - 1).coerceAtMost(17) downTo 0) {
			skyLightBitSet.set(i, skylightArrays[i] != null)
			skyLightBitSetInverse.set(i, skylightArrays[i] == null)
		}
		skyLightBitMasks = skyLightBitSet.toLongArray()
		skyLightBitMasksEmpty = skyLightBitSetInverse.toLongArray()

		val blockLightBitSet = BitSet()
		val blockLightBitSetInverse = BitSet()
		for (i in (blocklightArrays.size - 1).coerceAtMost(17) downTo 0) {
			blockLightBitSet.set(i, blocklightArrays[i] != null)
			blockLightBitSetInverse.set(i, blocklightArrays[i] == null)
		}
		blockLightBitMasks = blockLightBitSet.toLongArray()
		blockLightBitMasksEmpty = blockLightBitSetInverse.toLongArray()
	}

	@Throws(IOException::class)
	override fun serializePacket(): ByteArray {
		val buffer = ByteArrayOutputStream()
		val output = DataOutputStream(buffer)
		
		DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
		output.writeInt(chunkX)
		output.writeInt(chunkZ)

		DataTypeIO.writeVarInt(output, 1)
		DataTypeIO.writeVarInt(output, 4)
		val motionBlocking = chunk.heightMaps.getLongArray("MOTION_BLOCKING")
		DataTypeIO.writeVarInt(output, motionBlocking.size)
		for (l in motionBlocking) {
			output.writeLong(l)
		}

		val dataBuffer = ByteArrayOutputStream()
		val dataOut = DataOutputStream(dataBuffer)
		for (i in 0 until 16) {
			val section = chunk.getSection(i)
			if (section != null) {
				var counter: Short = 0
				for (x in 0 until 16) {
					for (z in 0 until 16) {
						for (y in 0 until 16) {
							val tag = section.getBlockStateAt(x, y, z)
							if (tag != null && tag.getString("Name") != "minecraft:air") {
								counter++
							}
						}
					}
				}
				dataOut.writeShort(counter.toInt())

				var newBits = 32 - Integer.numberOfLeadingZeros(section.palette.size() - 1)
				newBits = newBits.coerceAtLeast(4)
				
				if (newBits <= 8) {
					dataOut.writeByte(newBits)
					DataTypeIO.writeVarInt(dataOut, section.palette.size())
					for (tag in section.palette) {
						DataTypeIO.writeVarInt(dataOut, GeneratedBlockDataMappings.getGlobalPaletteIDFromState(tag))
					}

					val bits = BitSet.valueOf(section.blockStates)
					val shift = 64 % newBits
					val longsNeeded = ceil(4096.0 / (64 / newBits)).toInt()
					for (u in 64..bits.length() step 64) {
						BitsUtils.shiftAfter(bits, u - shift, shift)
					}

					val formattedLongs = bits.toLongArray()
					for (u in 0 until longsNeeded) {
						if (u < formattedLongs.size) {
							dataOut.writeLong(formattedLongs[u])
						} else {
							dataOut.writeLong(0)
						}
					}
				} else {
					try {
						dataOut.writeByte(16)
						val longsNeeded = 1024
						val list = LinkedList<Int>()
						for (y in 0 until 16) {
							for (z in 0 until 16) {
								for (x in 0 until 16) {
									list.add(GeneratedBlockDataMappings.getGlobalPaletteIDFromState(section.getBlockStateAt(x, y, z)))
								}
							}
						}
						val globalLongs = ArrayList<Long>()
						var currentLong: Long = 0
						var pos = 0
						var u = 0
						while (pos < longsNeeded) {
							if (u == 3) {
								globalLongs.add(currentLong)
								currentLong = 0
								u = 0
								pos++
							} else {
								u++
							}
							val id = if (list.isEmpty()) 0 else list.removeFirst()
							currentLong = currentLong shl 16
							currentLong = currentLong or id.toLong()
						}
						for (j in 0 until longsNeeded) {
							if (j < globalLongs.size) {
								dataOut.writeLong(globalLongs[j])
							} else {
								dataOut.writeLong(0)
							}
						}
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			} else {
				dataOut.writeShort(0)
				dataOut.writeByte(0)
				DataTypeIO.writeVarInt(dataOut, 0)
			}
			
			val biome = when (environment) {
				Environment.END -> 56
				Environment.NETHER -> 34
				else -> 40
			}
			dataOut.writeByte(0)
			DataTypeIO.writeVarInt(dataOut, biome)
		}

		val data = dataBuffer.toByteArray()
		DataTypeIO.writeVarInt(output, data.size)
		output.write(data)

		val tileEntities = chunk.tileEntities
		DataTypeIO.writeVarInt(output, tileEntities.size())
		for (each in tileEntities) {
			val x = each.getInt("x") % 16
			val y = each.getInt("y")
			val z = each.getInt("z") % 16
			val key = Key.key(chunk.getBlockStateAt(x, y, z).getString("Name"))
			val id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getId(key)
			if (id < 0) {
				IllegalStateException("Unable to get block entity type for $key").printStackTrace()
			}
			output.writeByte(((x and 15) shl 4) or (z and 15))
			output.writeShort(y)
			DataTypeIO.writeVarInt(output, id.coerceAtLeast(0))
			DataTypeIO.writeTag(output, each)
		}

		DataTypeIO.writeVarInt(output, skyLightBitMasks.size)
		for (l in skyLightBitMasks) output.writeLong(l)
		DataTypeIO.writeVarInt(output, blockLightBitMasks.size)
		for (l in blockLightBitMasks) output.writeLong(l)
		DataTypeIO.writeVarInt(output, skyLightBitMasksEmpty.size)
		for (l in skyLightBitMasksEmpty) output.writeLong(l)
		DataTypeIO.writeVarInt(output, blockLightBitMasksEmpty.size)
		for (l in blockLightBitMasksEmpty) output.writeLong(l)

		DataTypeIO.writeVarInt(output, skylightArrays.count { it != null })
		for (i in skylightArrays.size - 1 downTo 0) {
			skylightArrays[i]?.let {
				DataTypeIO.writeVarInt(output, 2048)
				for (b in it) output.writeByte(b.toInt())
			}
		}

		DataTypeIO.writeVarInt(output, blocklightArrays.count { it != null })
		for (i in blocklightArrays.size - 1 downTo 0) {
			blocklightArrays[i]?.let {
				DataTypeIO.writeVarInt(output, 2048)
				for (b in it) output.writeByte(b.toInt())
			}
		}

		return buffer.toByteArray()
	}
}

