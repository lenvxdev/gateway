package dev.lenvx.gateway.registry

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.utils.ClasspathResourcesUtils
import dev.lenvx.gateway.utils.CustomNBTUtils
import net.kyori.adventure.key.Key
import net.querz.nbt.tag.CompoundTag
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class RegistryCustom private constructor(val identifier: Key, val entries: Map<Key, CompoundTag>) {

    @Suppress("PatternValidation")
    constructor(identifier: String) : this(Key.key(identifier))

    @Suppress("PatternValidation")
    constructor(identifier: Key) : this(identifier, loadEntries(identifier))

    fun indexOf(key: Key): Int {
        return entries.keys.indexOf(key)
    }

    companion object {
        private val REGISTRIES = mutableMapOf<Key, RegistryCustom>()

        @JvmField val CAT_VARIANT = register("cat_variant")
        @JvmField val CHAT_TYPE = register("chat_type")
        @JvmField val CHICKEN_VARIANT = register("chicken_variant")
        @JvmField val COW_VARIANT = register("cow_variant")
        @JvmField val DAMAGE_TYPE = register("damage_type")
        @JvmField val DIMENSION_TYPE = register("dimension_type")
        @JvmField val FROG_VARIANT = register("frog_variant")
        @JvmField val PAINTING_VARIANT = register("painting_variant")
        @JvmField val PIG_VARIANT = register("pig_variant")
        @JvmField val TIMELINE = register("timeline")
        @JvmField val WOLF_SOUND_VARIANT = register("wolf_sound_variant")
        @JvmField val WOLF_VARIANT = register("wolf_variant")
        @JvmField val WORLDGEN_BIOME = register("worldgen/biome")
        @JvmField val ZOMBIE_NAUTILUS_VARIANT = register("zombie_nautilus_variant")

        private fun register(identifier: String): RegistryCustom {
            val registryCustom = RegistryCustom(identifier)
            REGISTRIES[registryCustom.identifier] = registryCustom
            return registryCustom
        }

        @JvmStatic
        fun getRegistry(identifier: Key): RegistryCustom? = REGISTRIES[identifier]

        @JvmStatic
        fun getRegistries(): Collection<RegistryCustom> = REGISTRIES.values

        private fun loadEntries(identifier: Key): Map<Key, CompoundTag> {
            val entries = linkedMapOf<Key, CompoundTag>()
            val pathStart = "data/${identifier.namespace()}/${identifier.value()}/"
            val pattern = Pattern.compile(Pattern.quote(pathStart) + ".*")
            for (path in ClasspathResourcesUtils.getResources(pattern)) {
                if (path.endsWith(".json")) {
                    try {
                        val inputStream = Gateway::class.java.classLoader.getResourceAsStream(path) ?: continue
                        val entryName = path.substring(path.lastIndexOf('/') + 1, path.lastIndexOf("."))
                        val entryKey = Key.key(identifier.namespace(), entryName)
                        InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                            val jsonObject = JSONParser().parse(reader) as JSONObject
                            val value = CustomNBTUtils.getCompoundTagFromJson(jsonObject)
                            entries[entryKey] = value
                        }
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
            }
            return entries
        }
    }
}


