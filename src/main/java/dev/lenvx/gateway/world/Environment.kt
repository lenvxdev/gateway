package dev.lenvx.gateway.world

import net.kyori.adventure.key.Key

data class Environment private constructor(val key: Key, val hasSkyLight: Boolean) {

	companion object {
		@JvmField val NORMAL = Environment(Key.key("minecraft:overworld"), true)
		@JvmField val NETHER = Environment(Key.key("minecraft:the_nether"), false)
		@JvmField val END = Environment(Key.key("minecraft:the_end"), false)

		@JvmField val REGISTERED_ENVIRONMENTS = mutableSetOf<Environment>()

		@JvmStatic
		fun fromKey(key: Key): Environment? {
			return when (key) {
				NORMAL.key -> NORMAL
				NETHER.key -> NETHER
				END.key -> END
				else -> REGISTERED_ENVIRONMENTS.find { it.key == key }
			}
		}

		@JvmStatic
		@Deprecated("Use createCustom(Key, Boolean)", ReplaceWith("createCustom(key, true)"))
		fun createCustom(key: Key): Environment = createCustom(key, true)

		@JvmStatic
		fun createCustom(key: Key, hasSkyLight: Boolean): Environment {
			if (REGISTERED_ENVIRONMENTS.any { it.key == key }) {
				throw IllegalArgumentException("An Environment is already created with this Key")
			}
			val environment = Environment(key, hasSkyLight)
			REGISTERED_ENVIRONMENTS.add(environment)
			return environment
		}

		@JvmStatic
		fun getCustom(key: Key): Environment? = REGISTERED_ENVIRONMENTS.find { it.key == key }
	}

	fun hasSkyLight(): Boolean = hasSkyLight

}

