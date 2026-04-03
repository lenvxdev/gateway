package dev.lenvx.gateway.registry

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import dev.lenvx.gateway.Gateway
import net.kyori.adventure.key.Key
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.LinkedList

sealed class BuiltInRegistries {

	abstract fun getId(key: Key): Int

	companion object {
		@JvmField
		val BLOCK_ENTITY_TYPE: BlockEntityRegistry
		@JvmField
		val ITEM_REGISTRY: ItemRegistry
		@JvmField
		val MENU_REGISTRY: MenuRegistry
		@JvmField
		val DATA_COMPONENT_TYPE: DataComponentTypeRegistry

		init {
			val name = "reports/registries.json"

			val blockEntityTypeMap = mutableMapOf<Key, Int>()
			var defaultItemKey: Key? = null
			val itemIds: BiMap<Key, Int> = HashBiMap.create()
			val menuIds = mutableMapOf<Key, Int>()
			val dataComponentTypeIds: BiMap<Key, Int> = HashBiMap.create()

			val inputStream = Gateway::class.java.classLoader.getResourceAsStream(name)
				?: throw RuntimeException("Failed to load $name from jar!")

			try {
				InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
					val json = JSONParser().parse(reader) as JSONObject

					val blockEntityJson = (json["minecraft:block_entity_type"] as JSONObject)["entries"] as JSONObject
					for (obj in blockEntityJson.keys) {
						val key = obj.toString()
						val id = (blockEntityJson[key] as JSONObject)["protocol_id"] as Number
						blockEntityTypeMap[Key.key(key)] = id.toInt()
					}

					val itemJson = json["minecraft:item"] as JSONObject
					defaultItemKey = Key.key(itemJson["default"] as String)
					val itemEntriesJson = itemJson["entries"] as JSONObject
					for (obj in itemEntriesJson.keys) {
						val key = obj.toString()
						val id = (itemEntriesJson[key] as JSONObject)["protocol_id"] as Number
						itemIds[Key.key(key)] = id.toInt()
					}

					val menuEntriesJson = (json["minecraft:menu"] as JSONObject)["entries"] as JSONObject
					for (obj in menuEntriesJson.keys) {
						val key = obj.toString()
						val id = (menuEntriesJson[key] as JSONObject)["protocol_id"] as Number
						menuIds[Key.key(key)] = id.toInt()
					}

					val dataComponentTypeEntriesJson = (json["minecraft:data_component_type"] as JSONObject)["entries"] as JSONObject
					for (obj in dataComponentTypeEntriesJson.keys) {
						val key = obj.toString()
						val id = (dataComponentTypeEntriesJson[key] as JSONObject)["protocol_id"] as Number
						dataComponentTypeIds[Key.key(key)] = id.toInt()
					}
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
			BLOCK_ENTITY_TYPE = BlockEntityRegistry(blockEntityTypeMap)
			ITEM_REGISTRY = ItemRegistry(defaultItemKey, itemIds)
			MENU_REGISTRY = MenuRegistry(menuIds)
			DATA_COMPONENT_TYPE = DataComponentTypeRegistry(dataComponentTypeIds)
		}
	}

	class BlockEntityRegistry(private val blockEntityType: Map<Key, Int>) : BuiltInRegistries() {
		override fun getId(key: Key): Int {
			val exact = blockEntityType[key]
			if (exact != null) {
				return exact
			}
			val toTest = LinkedList<String>()
			toTest.add(key.value())
			if (key.value().contains("head")) {
				toTest.add("skull")
			}
			for (entry in blockEntityType.entries) {
				val registryKey = entry.key
				for (testStr in toTest) {
					if (registryKey.namespace() == key.namespace() && (testStr.contains(registryKey.value()) || registryKey.value().contains(testStr))) {
						return entry.value
					}
				}
			}
			return -1
		}
	}

	class ItemRegistry(val defaultKey: Key?, private val itemIds: BiMap<Key, Int>) : BuiltInRegistries() {
		override fun getId(key: Key): Int {
			val id = itemIds[key]
			if (id != null) {
				return id
			}
			return if (defaultKey == null) 0 else itemIds.getOrDefault(defaultKey, 0)
		}

		fun fromId(id: Int): Key? {
			return itemIds.inverse().getOrDefault(id, defaultKey)
		}
	}

	class MenuRegistry(private val menuIds: Map<Key, Int>) : BuiltInRegistries() {
		override fun getId(key: Key): Int {
			return menuIds.getOrDefault(key, -1)
		}
	}

	class DataComponentTypeRegistry(private val dataComponentTypeIds: BiMap<Key, Int>) : BuiltInRegistries() {
		override fun getId(key: Key): Int {
			return dataComponentTypeIds.getOrDefault(key, -1)
		}

		fun fromId(id: Int): Key? {
			return dataComponentTypeIds.inverse()[id]
		}
	}

}


