package dev.lenvx.gateway.utils

enum class GameMode(val id: Int, @get:JvmName("getName") val modeName: String) {

	SURVIVAL(0, "survival"),
	CREATIVE(1, "creative"),
	ADVENTURE(2, "adventure"),
	SPECTATOR(3, "spectator");

	companion object {
		@JvmStatic
		fun fromId(id: Int): GameMode? = values().find { it.id == id }

		@JvmStatic
		fun fromName(name: String): GameMode? = values().find { it.modeName.equals(name, ignoreCase = true) }
	}
}

