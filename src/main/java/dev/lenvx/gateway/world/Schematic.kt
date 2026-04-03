package dev.lenvx.gateway.world

import dev.lenvx.gateway.utils.SchematicConversionUtils
import net.querz.mca.Chunk
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.ListTag

object Schematic {

	private const val MAX_SIZE = 1024
	private const val MAX_HEIGHT = 512

	@JvmStatic
	fun toWorld(name: String, environment: Environment, nbt: CompoundTag): World {
		return if (nbt.containsKey("Version") || nbt.containsKey("Palette")) {
			loadSponge(name, environment, nbt)
		} else if (nbt.containsKey("Blocks")) {
			loadMCEdit(name, environment, nbt)
		} else {
			throw IllegalArgumentException("Unknown schematic format. Missing 'Palette' or 'Blocks' tag.")
		}
	}

	private fun loadSponge(name: String, environment: Environment, nbt: CompoundTag): World {
		val width = nbt.getShort("Width").toInt() and 0xFFFF
		val height = nbt.getShort("Height").toInt() and 0xFFFF
		val length = nbt.getShort("Length").toInt() and 0xFFFF

		validateDimensions(width, height, length)

		val blockData = nbt.getByteArray("BlockData")
		val palette = nbt.getCompoundTag("Palette") ?: throw IllegalArgumentException("Sponge schematic missing 'Palette' tag.")

		val mapping = mutableMapOf<Int, String>()
		for (key in palette.keySet()) {
			mapping[palette.getInt(key)] = key
		}

		val world = World(name, width, length, environment)
		val blockEntityMap = indexBlockEntities(nbt)

		var index = 0
		var i = 0
		while (i < blockData.size && index < width * height * length) {
			var value = 0
			var varIntLength = 0
			while (true) {
				if (i >= blockData.size) {
					throw RuntimeException("Unexpected end of BlockData while decoding VarInt at index $index")
				}
				value = value or ((blockData[i].toInt() and 127) shl (varIntLength++ * 7))
				if (varIntLength > 5) {
					throw RuntimeException("VarInt too big (probably corrupted data)")
				}
				if ((blockData[i].toInt() and 128) != 128) {
					i++
					break
				}
				i++
			}

			val y = index / (width * length)
			val z = (index % (width * length)) / width
			val x = (index % (width * length)) % width

			val blockName = mapping[value]
			if (blockName != null) {
				world.setBlock(x, y, z, blockName)
			}

			applyBlockEntities(world, x, y, z, blockEntityMap)
			index++
		}

		finalizeWorld(world)
		return world
	}

	private fun loadMCEdit(name: String, environment: Environment, nbt: CompoundTag): World {
		val width = nbt.getShort("Width").toInt() and 0xFFFF
		val height = nbt.getShort("Height").toInt() and 0xFFFF
		val length = nbt.getShort("Length").toInt() and 0xFFFF

		validateDimensions(width, height, length)

		val blocks = nbt.getByteArray("Blocks")
		val data = nbt.getByteArray("Data")
		val addBlocks = if (nbt.containsKey("AddBlocks")) nbt.getByteArray("AddBlocks") else null

		val expectedSize = width * height * length
		if (blocks.size < expectedSize) {
			throw IllegalArgumentException("MCEdit schematic 'Blocks' array is too small (Expected: $expectedSize, Got: ${blocks.size})")
		}
		if (data.size < expectedSize) {
			throw IllegalArgumentException("MCEdit schematic 'Data' array is too small (Expected: $expectedSize, Got: ${data.size})")
		}
		if (addBlocks != null && addBlocks.size < (expectedSize + 1) / 2) {
			throw IllegalArgumentException("MCEdit schematic 'AddBlocks' array is too small (Expected: ${(expectedSize + 1) / 2}, Got: ${addBlocks.size})")
		}

		val world = World(name, width, length, environment)
		val blockEntityMap = indexBlockEntities(nbt)

		for (y in 0 until height) {
			for (z in 0 until length) {
				for (x in 0 until width) {
					val index = (y * length + z) * width + x
					var id = blocks[index].toInt() and 0xFF
					if (addBlocks != null) {
						val addIndex = index shr 1
						id = if ((index and 1) == 0) {
							id or ((addBlocks[addIndex].toInt() and 0x0F) shl 8)
						} else {
							id or ((addBlocks[addIndex].toInt() and 0xF0) shl 4)
						}
					}
					val blockData = if (data.size > index) (data[index].toInt() and 0x0F) else 0
					val blockName = SchematicConversionUtils.getLegacyBlock(id, blockData)
					world.setBlock(x, y, z, blockName)
					applyBlockEntities(world, x, y, z, blockEntityMap)
				}
			}
		}

		finalizeWorld(world)
		return world
	}

	private fun validateDimensions(width: Int, height: Int, length: Int) {
		if (width > MAX_SIZE || length > MAX_SIZE || height > MAX_HEIGHT) {
			throw IllegalArgumentException("Schematic dimensions too large: ${width}x${height}x${length} (Max: ${MAX_SIZE}x${MAX_HEIGHT}x${MAX_SIZE})")
		}
	}

	private fun indexBlockEntities(nbt: CompoundTag): Map<Long, ListTag<CompoundTag>> {
		val map = mutableMapOf<Long, ListTag<CompoundTag>>()
		if (nbt.containsKey("BlockEntities")) {
			val blockEntities = nbt.getListTag("BlockEntities").asTypedList(CompoundTag::class.java)
			for (tag in blockEntities) {
				val pos = tag.getIntArray("Pos")
				if (pos.size == 3) {
					val key = blockPosToLong(pos[0], pos[1], pos[2])
					map.getOrPut(key) { ListTag(CompoundTag::class.java) }.add(tag)
				}
			}
		} else if (nbt.containsKey("TileEntities")) {
			val tileEntities = nbt.getListTag("TileEntities").asTypedList(CompoundTag::class.java)
			for (tag in tileEntities) {
				if (tag.containsKey("x") && tag.containsKey("y") && tag.containsKey("z")) {
					val key = blockPosToLong(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"))
					map.getOrPut(key) { ListTag(CompoundTag::class.java) }.add(tag)
				}
			}
		}
		return map
	}

	private fun applyBlockEntities(world: World, x: Int, y: Int, z: Int, blockEntityMap: Map<Long, ListTag<CompoundTag>>) {
		val key = blockPosToLong(x, y, z)
		val tags = blockEntityMap[key]
		if (tags != null) {
			val chunk = world.getChunkAtWorldPos(x, z)
			if (chunk != null) {
				val chunkTags = chunk.tileEntities
				for (tag in tags) {
					chunkTags.add(SchematicConversionUtils.toTileEntityTag(tag))
				}
				chunk.setTileEntities(chunkTags)
			}
		}
	}

	private fun blockPosToLong(x: Int, y: Int, z: Int): Long {
		return (x.toLong() and 0x3FFFFFF) shl 38 or ((z.toLong() and 0x3FFFFFF) shl 12) or (y.toLong() and 0xFFF)
	}

	private fun finalizeWorld(world: World) {
		for (chunkArray in world.chunks) {
			for (chunk in chunkArray) {
				chunk.setHeightMaps(World.HEIGHT_MAP.clone())
				chunk.setBiomes(IntArray(1024))
				chunk.cleanupPalettesAndBlockStates()
			}
		}
		world.lightEngineBlock.updateWorld()
		world.lightEngineSky?.updateWorld()
	}
}

