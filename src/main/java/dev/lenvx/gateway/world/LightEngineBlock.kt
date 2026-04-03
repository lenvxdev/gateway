package dev.lenvx.gateway.world

class LightEngineBlock(private val world: World) : LightEngine() {

	private var blockLightArray = Array(world.chunkWidth * 16) { Array(16 * 18) { ByteArray(world.chunkLength * 16) } }

	init {
		updateWorld()
	}

	fun updateWorld() {
		blockLightArray = Array(world.chunkWidth * 16) { Array(16 * 18) { ByteArray(world.chunkLength * 16) } }
		for (x in 0 until world.width) {
			for (y in 0 until 256) {
				for (z in 0 until world.length) {
					updateBlock(x, y, z)
				}
			}
		}
	}

	private fun updateBlock(x: Int, y: Int, z: Int) {
		val block = world.getBlock(x, y, z)
		val lightLevel = getBlockLight(block)
		if (lightLevel > 0) {
			propagate(lightLevel, x, y, z)
		}
	}

	private fun propagate(level: Int, x: Int, y: Int, z: Int) {
		try {
			if (blockLightArray[x][y + 16][z].toInt() < level) {
				blockLightArray[x][y + 16][z] = level.toByte()
				if (level > 1) {
					try { propagate(level - 1, x + 1, y, z) } catch (ignored: ArrayIndexOutOfBoundsException) {}
					try { propagate(level - 1, x - 1, y, z) } catch (ignored: ArrayIndexOutOfBoundsException) {}
					try { propagate(level - 1, x, y + 1, z) } catch (ignored: ArrayIndexOutOfBoundsException) {}
					try { propagate(level - 1, x, y - 1, z) } catch (ignored: ArrayIndexOutOfBoundsException) {}
					try { propagate(level - 1, x, y, z + 1) } catch (ignored: ArrayIndexOutOfBoundsException) {}
					try { propagate(level - 1, x, y, z - 1) } catch (ignored: ArrayIndexOutOfBoundsException) {}
				}
			}
		} catch (ignored: ArrayIndexOutOfBoundsException) {}
	}

	fun getBlockLightBitMask(chunkX: Int, chunkZ: Int): List<Array<Byte>> {
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
						var bit = blockLightArray[x][y][z].toInt()
						bit = bit shl 4
						bit = bit or blockLightArray[x + 1][y][z].toInt()
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

