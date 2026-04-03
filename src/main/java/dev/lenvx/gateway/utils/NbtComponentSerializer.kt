package dev.lenvx.gateway.utils

import com.google.gson.*
import com.google.gson.internal.LazilyParsedNumber
import net.querz.nbt.tag.*
import java.util.*


object NbtComponentSerializer {

	private val BOOLEAN_TYPES = setOf(
		"interpret",
		"bold",
		"italic",
		"underlined",
		"strikethrough",
		"obfuscated"
	)
	private val COMPONENT_TYPES = listOf(
		Pair("text", "text"),
		Pair("translatable", "translate"),
		Pair("score", "score"),
		Pair("selector", "selector"),
		Pair("keybind", "keybind"),
		Pair("nbt", "nbt")
	)

	@JvmStatic
	fun tagComponentToJson(tag: Tag<*>): JsonElement? {
		return convertToJson(null, tag)
	}

	@JvmStatic
	fun jsonComponentToTag(component: JsonElement): Tag<*>? {
		return convertToTag(component)
	}

	private fun convertToTag(element: JsonElement?): Tag<*>? {
		if (element == null || element.isJsonNull) {
			return null
		} else if (element.isJsonObject) {
			val tag = CompoundTag()
			val jsonObject = element.asJsonObject
			for (entry in jsonObject.entrySet()) {
				convertObjectEntry(entry.key, entry.value, tag)
			}
			addComponentType(jsonObject, tag)
			return tag
		} else if (element.isJsonArray) {
			return convertJsonArray(element.asJsonArray)
		} else if (element.isJsonPrimitive) {
			val primitive = element.asJsonPrimitive
			if (primitive.isString) {
				return StringTag(primitive.asString)
			} else if (primitive.isBoolean) {
				return ByteTag(if (primitive.asBoolean) 1.toByte() else 0.toByte())
			}

			val number = primitive.asNumber
			return when (number) {
				is Int -> IntTag(number)
				is Byte -> ByteTag(number)
				is Short -> ShortTag(number)
				is Long -> LongTag(number)
				is Double -> DoubleTag(number)
				is Float -> FloatTag(number)
				is LazilyParsedNumber -> IntTag(number.toInt())
				else -> IntTag(number.toInt())
			}
		}
		throw IllegalArgumentException("Unhandled json type ${element::class.java.simpleName} with value ${element.asString}")
	}

	@Suppress("UNCHECKED_CAST")
	private fun convertJsonArray(array: JsonArray): ListTag<*> {
		val listTag = ListTag.createUnchecked(EndTag::class.java as Class<out Tag<*>>)
		var singleType = true
		for (entry in array) {
			val convertedEntryTag = convertToTag(entry) ?: continue
			val listTypeClass = listTag.typeClass
			if (listTypeClass != null && listTypeClass != convertedEntryTag::class.java) {
				singleType = false
				break
			}
			(listTag as ListTag<Tag<*>>).add(convertedEntryTag as Tag<*>)
		}

		if (singleType) {
			return listTag
		}

		val processedListTag = ListTag.createUnchecked(EndTag::class.java as Class<out Tag<*>>)
		for (entry in array) {
			val convertedTag = convertToTag(entry) ?: continue
			if (convertedTag is CompoundTag) {
				(processedListTag as ListTag<Tag<*>>).add(convertedTag)
				continue
			}

			val compoundTag = CompoundTag()
			compoundTag.put("type", StringTag("text"))
			if (convertedTag is ListTag<*>) {
				compoundTag.put("text", StringTag())
				compoundTag.put("extra", convertedTag.clone())
			} else {
				compoundTag.put("text", StringTag(stringValue(convertedTag)))
			}
			(processedListTag as ListTag<Tag<*>>).add(compoundTag)
		}
		return processedListTag
	}

	private fun convertObjectEntry(key: String, value: JsonElement, tag: CompoundTag) {
		if (key == "contents" && value.isJsonObject) {
			val hoverEvent = value.asJsonObject
			val idElement = hoverEvent["id"]
			if (idElement != null && idElement.isJsonPrimitive) {
				val uuid = parseUUID(idElement.asString)
				if (uuid != null) {
					hoverEvent.remove("id")
					val convertedTag = convertToTag(value) as CompoundTag
					convertedTag.put("id", IntArrayTag(toIntArray(uuid)))
					tag.put(key, convertedTag)
					return
				}
			}
		}
		tag.put(key, convertToTag(value))
	}

	private fun addComponentType(jsonObject: JsonObject, tag: CompoundTag) {
		if (jsonObject.has("type")) return

		for (pair in COMPONENT_TYPES) {
			if (jsonObject.has(pair.second)) {
				tag.put("type", StringTag(pair.first))
				return
			}
		}
	}

	private fun convertToJson(key: String?, tag: Tag<*>?): JsonElement? {
		if (tag == null) return null
		return when (tag) {
			is CompoundTag -> {
				val jsonObject = JsonObject()
				for (entry in tag.entrySet()) {
					convertCompoundTagEntry(entry.key, entry.value, jsonObject)
				}
				if (key != "value") {
					removeComponentType(jsonObject)
				}
				jsonObject
			}
			is ListTag<*> -> {
				val array = JsonArray()
				for (listEntry in tag) {
					array.add(convertToJson(null, listEntry))
				}
				array
			}
			is NumberTag<*> -> {
				if (key != null && BOOLEAN_TYPES.contains(key)) {
					JsonPrimitive(tag.asByte() != 0.toByte())
				} else {
					when (tag) {
						is ByteTag -> JsonPrimitive(tag.asByte())
						is ShortTag -> JsonPrimitive(tag.asShort())
						is IntTag -> JsonPrimitive(tag.asInt())
						is LongTag -> JsonPrimitive(tag.asLong())
						is FloatTag -> JsonPrimitive(tag.asFloat())
						is DoubleTag -> JsonPrimitive(tag.asDouble())
						else -> JsonPrimitive(tag.asDouble())
					}
				}
			}
			is StringTag -> JsonPrimitive(tag.value)
			is ByteArrayTag -> {
				val array = JsonArray()
				for (byte in tag.value) array.add(byte)
				array
			}
			is IntArrayTag -> {
				val array = JsonArray()
				for (int in tag.value) array.add(int)
				array
			}
			is LongArrayTag -> {
				val array = JsonArray()
				for (long in tag.value) array.add(long)
				array
			}
			else -> throw IllegalArgumentException("Unhandled tag type ${tag::class.java.simpleName}")
		}
	}

	private fun convertCompoundTagEntry(key: String, tag: Tag<*>, jsonObject: JsonObject) {
		if (key == "contents" && tag is CompoundTag) {
			val idTag = tag["id"]
			if (idTag is IntArrayTag) {
				tag.remove("id")
				val convertedElement = convertToJson(key, tag) as JsonObject
				val uuid = fromIntArray(idTag.value)
				convertedElement.addProperty("id", uuid.toString())
				jsonObject.add(key, convertedElement)
				return
			}
		}
		jsonObject.add(if (key.isEmpty()) "text" else key, convertToJson(key, tag))
	}

	private fun removeComponentType(jsonObject: JsonObject) {
		val typeElement = jsonObject.remove("type")
		if (typeElement == null || !typeElement.isJsonPrimitive) return

		val typeString = typeElement.asString
		for (pair in COMPONENT_TYPES) {
			if (pair.first != typeString) {
				jsonObject.remove(pair.second)
			}
		}
	}

	@JvmStatic
	fun fromIntArray(parts: IntArray): UUID {
		if (parts.size != 4) return UUID(0, 0)
		return UUID(
			parts[0].toLong() shl 32 or (parts[1].toLong() and 0xFFFFFFFFL),
			parts[2].toLong() shl 32 or (parts[3].toLong() and 0xFFFFFFFFL)
		)
	}

	@JvmStatic
	fun toIntArray(uuid: UUID): IntArray {
		return toIntArray(uuid.mostSignificantBits, uuid.leastSignificantBits)
	}

	@JvmStatic
	fun toIntArray(msb: Long, lsb: Long): IntArray {
		return intArrayOf(
			(msb shr 32).toInt(),
			msb.toInt(),
			(lsb shr 32).toInt(),
			lsb.toInt()
		)
	}

	@JvmStatic
	fun parseUUID(uuidString: String): UUID? {
		return try {
			UUID.fromString(uuidString)
		} catch (e: IllegalArgumentException) {
			null
		}
	}

	private fun stringValue(tag: Tag<*>): String {
		return when (tag) {
			is ByteArrayTag -> tag.value.contentToString()
			is ByteTag -> tag.asByte().toString()
			is DoubleTag -> tag.asDouble().toString()
			is FloatTag -> tag.asFloat().toString()
			is IntArrayTag -> tag.value.contentToString()
			is IntTag -> tag.asInt().toString()
			is LongArrayTag -> tag.value.contentToString()
			is LongTag -> tag.asLong().toString()
			is ShortTag -> tag.asShort().toString()
			is StringTag -> tag.value
			else -> tag.valueToString()
		}
	}
}

