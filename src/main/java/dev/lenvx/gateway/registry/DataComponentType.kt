package dev.lenvx.gateway.registry

import com.google.gson.JsonElement
import dev.lenvx.gateway.utils.NbtComponentSerializer
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.querz.nbt.tag.Tag
import java.util.HashMap
import java.util.function.Function

class DataComponentType<T>(val key: Key, val codec: DataComponentCodec<T>) {

    constructor(key: String, codec: DataComponentCodec<T>) : this(Key.key(key), codec)

    companion object {
        private val REGISTERED_TYPES: MutableMap<Key, DataComponentType<*>> = HashMap()

        @JvmField
        val CUSTOM_NAME = register(DataComponentType("custom_name", DataComponentCodec<Component>({ component ->
            val element: JsonElement = GsonComponentSerializer.gson().serializeToTree(component)
            NbtComponentSerializer.jsonComponentToTag(element)!!
        }, { tag ->
            val element: JsonElement = NbtComponentSerializer.tagComponentToJson(tag)!!
            GsonComponentSerializer.gson().deserializeFromTree(element)
        })))

        @JvmStatic
        fun <T> register(type: DataComponentType<T>): DataComponentType<T> {
            REGISTERED_TYPES[type.key] = type
            return type
        }

        @JvmStatic
        fun isKnownType(key: Key): Boolean {
            return REGISTERED_TYPES.containsKey(key)
        }
    }

    class DataComponentCodec<T>(
        private val encodeFunc: Function<T, Tag<*>>,
        private val decodeFunc: Function<Tag<*>, T>
    ) {
        fun encode(t: T): Tag<*> = encodeFunc.apply(t)
        fun decode(tag: Tag<*>): T = decodeFunc.apply(tag)
    }
}

