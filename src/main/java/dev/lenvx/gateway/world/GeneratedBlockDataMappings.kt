package dev.lenvx.gateway.world

import dev.lenvx.gateway.Gateway
import net.querz.nbt.io.SNBTUtil
import net.querz.nbt.tag.CompoundTag
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object GeneratedBlockDataMappings {

	private val globalPalette: JSONObject

	init {
		val block = "reports/blocks.json"
		val inputStream = Gateway::class.java.classLoader.getResourceAsStream(block)
				?: throw RuntimeException("Failed to load $block from jar!")
		
		globalPalette = inputStream.use { stream ->
			InputStreamReader(stream, StandardCharsets.UTF_8).use { reader ->
				JSONParser().parse(reader) as JSONObject
			}
		}
	}

	@JvmStatic
	fun getGlobalPaletteIDFromState(tag: CompoundTag): Int {
		return try {
			val blockName = tag.getString("Name")
			val data = globalPalette[blockName] as? JSONObject ?: throw IllegalStateException("Block $blockName not found in palette")
			
			val propertiesObj = data["properties"]
			val states = data["states"] as JSONArray
			
			if (propertiesObj == null) {
				return ((states[0] as JSONObject)["id"] as Number).toInt()
			}

			if (tag.containsKey("Properties")) {
				val blockProp = tag.getCompoundTag("Properties")
				val blockState = mutableMapOf<String, String>()
				for (key in blockProp.keySet()) {
					blockState[key] = blockProp.getString(key)
				}

				for (entry in states) {
					val jsonObj = entry as JSONObject
					val jsonProperties = jsonObj["properties"] as JSONObject
					if (jsonProperties.keys.all { key -> blockState[key as String] == jsonProperties[key].toString() }) {
						return (jsonObj["id"] as Number).toInt()
					}
				}
			}

			for (entry in states) {
				val jsonObj = entry as JSONObject
				if (jsonObj.containsKey("default") && (jsonObj["default"] as Boolean)) {
					return (jsonObj["id"] as Number).toInt()
				}
			}

			throw IllegalStateException()
		} catch (e: Throwable) {
			val snbt = try {
				SNBTUtil.toSNBT(tag)
			} catch (e1: Exception) {
				tag.toString()
			}
			IllegalStateException("Unable to get global palette id for $snbt (Is this scheme created in the same Minecraft version as Gateway?)", e).printStackTrace()
			0
		}
	}

}


