package dev.lenvx.gateway.world

abstract class LightEngine {

	companion object {
		private val blockLightLevelMapping = mapOf(
			"minecraft:beacon" to 15.toByte(),
			"minecraft:torch" to 15.toByte(),
			"minecraft:sea_lantern" to 15.toByte(),
			"minecraft:end_rod" to 15.toByte(),
			"minecraft:fire" to 15.toByte(),
			"minecraft:lava" to 15.toByte(),
			"minecraft:lantern" to 15.toByte(),
			"minecraft:soul_lantern" to 10.toByte(),
			"minecraft:glowstone" to 15.toByte(),
			"minecraft:campfire" to 15.toByte(),
			"minecraft:soul_campfire" to 10.toByte()
		)

		@JvmStatic
		fun getBlockLight(block: BlockState): Int {
			return blockLightLevelMapping[block.type.toString()]?.toInt() ?: 0
		}
	}

}

