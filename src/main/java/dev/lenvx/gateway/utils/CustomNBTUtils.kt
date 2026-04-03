package dev.lenvx.gateway.utils

import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.ListTag
import org.json.simple.JSONArray
import org.json.simple.JSONObject

object CustomNBTUtils {

	@JvmStatic
	fun getCompoundTagFromJson(json: JSONObject): CompoundTag {
		val tag = CompoundTag()
		for (obj in json.keys) {
			val key = obj as String
			when (val rawValue = json[key]) {
				is JSONObject -> tag.put(key, getCompoundTagFromJson(rawValue))
				is JSONArray -> tag.put(key, getListTagFromJson(rawValue))
				is Boolean -> tag.putBoolean(key, rawValue)
				is Long -> tag.putLong(key, rawValue)
				is Double -> tag.putDouble(key, rawValue)
				is String -> tag.putString(key, rawValue)
			}
		}
		return tag
	}

	@JvmStatic
	fun getListTagFromJson(json: JSONArray): ListTag<*> {
		if (json.isEmpty()) {
			return ListTag.createUnchecked(null)
		}
		val listTag = ListTag.createUnchecked(null)
		for (rawValue in json) {
			when (rawValue) {
				is JSONObject -> listTag.addUnchecked(getCompoundTagFromJson(rawValue))
				is JSONArray -> listTag.addUnchecked(getListTagFromJson(rawValue))
				is Boolean -> listTag.addBoolean(rawValue)
				is Long -> listTag.addLong(rawValue)
				is Double -> listTag.addDouble(rawValue)
				is String -> listTag.addString(rawValue)
			}
		}
		return listTag
	}

}

