package dev.lenvx.gateway.world

class LightEngineSky(private val world: World) : LightEngine() {

	private var skyLightArray = Array(world.chunkWidth * 16) { Array(16 * 18) { ByteArray(world.chunkLength * 16) } }

	init {
		updateWorld()
	}

	fun updateWorld() {
		skyLightArray = Array(world.chunkWidth * 16) { Array(16 * 18) { ByteArray(world.chunkLength * 16) } }
		for (x in 0 until world.width) {
			for (z in 0 until world.length) {
				updateColumn(x, z)
			}
		}
	}

	private fun updateColumn(x: Int, z: Int) {
		for (y in 272 downTo 256) {
			propagate(15, x, y, z)
		}
		for (y in 255 downTo 0) {
			val block = world.getBlock(x, y, z)
			if (block.type.toString() != "minecraft:air") {
				break
			}
			propagate(15, x, y, z)
		}
	}

	private fun propagate(level: Int, x: Int, y: Int, z: Int) {
		try {
			if (skyLightArray[x][y + 16][z].toInt() < level) {
				skyLightArray[x][y + 16][z] = level.toByte()
				if (level > 1) {
					try { propagate(level - 1, x + 1, y, z) } catch (ignored: ArrayIndexOutOfBoundsException) {}
					try { propagate(level - 1, x - 1, y, z) } catch (ignored: ArrayIndexOutOfBoundsException) {}
					try { propagate(level - 1, x, y + 1, z) } catch (ignored: ArrayIndexOutOfBoundsException) {}
					try { propagate(level - 1, x, y, z + 1) } catch (ignored: ArrayIndexOutOfBoundsException) {}
					try { propagate(level - 1, x, y, z - 1) } catch (ignored: ArrayIndexOutOfBoundsException) {}
				}
			}
		} catch (ignored: ArrayIndexOutOfBoundsException) {}
	}

	fun getSkyLightBitMask(chunkX: Int, chunkZ: Int): List<Array<Byte>> {
		val subchunks = mutableListOf<Array<Byte>>()
		val startX = chunkX * 16
		val endingX = startX + 16
		val startZ = chunkZ * 16
		val endingZ = startZ + 16

		for (sub in 17 downTo 0) {
			val array = arrayOfNulls<Byte>(2048)
			var i = 0
			for (y in sub * 16 until (sub * 16) + 16) {
				for (z in startZ until endingZ) {
					for (x in startX until endingX step 2) {
						var bit = skyLightArray[x][y][z].toInt()
						bit = bit shl 4
						bit = bit or skyLightArray[x + 1][y][z].toInt()
						array[i++] = bit.toByte()
					}
				}
			}
			@Suppress("UNCHECKED_CAST")
			subchunks.add(array as Array<Byte>)
		}

		return subchunks
	}
}

