package dev.lenvx.gateway.world

import net.kyori.adventure.key.Key
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.StringTag

data class BlockState(private val tag: CompoundTag) {

	fun toCompoundTag(): CompoundTag = tag

	var type: Key
		get() = Key.key(tag.getString("Name"))
		set(value) {
			tag.putString("Name", value.toString())
		}

	fun getProperties(): Map<String, String> {
		val mapping = mutableMapOf<String, String>()
		val properties = tag.getCompoundTag("Properties")
		if (properties != null) {
			for (entry in properties) {
				mapping[entry.key] = (entry.value as StringTag).value
			}
		}
		return mapping
	}

	fun getProperty(key: String): String? {
		val value = tag.getCompoundTag("Properties")?.get(key)
		return (value as? StringTag)?.value
	}

	fun setProperties(mapping: Map<String, String>) {
		val properties = CompoundTag()
		for ((key, value) in mapping) {
			properties.putString(key, value)
		}
		tag.put("Properties", properties)
	}

	fun <T> setProperty(key: String, value: T) {
		val properties = tag.getCompoundTag("Properties") ?: CompoundTag().also { tag.put("Properties", it) }
		properties.putString(key, value.toString())
	}

}

