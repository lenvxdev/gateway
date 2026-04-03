package dev.lenvx.gateway.entity

import dev.lenvx.gateway.player.Player
import net.kyori.adventure.key.Key
import java.util.*

enum class EntityType(
	private val nameString: String?,
	val entityClass: Class<out Entity>?,
	val typeId: Short,
	val isSpawnable: Boolean = true
) {

	ARMOR_STAND("armor_stand", ArmorStand::class.java, 1),
	PLAYER("player", Player::class.java, 106, false),
	UNKNOWN(null, null, -1, false);

	val isAlive: Boolean = entityClass != null && LivingEntity::class.java.isAssignableFrom(entityClass)
	val key: Key? = nameString?.let { Key.key(Key.MINECRAFT_NAMESPACE, it) }

	companion object {
		private val NAME_MAP: Map<String, EntityType>
		private val ID_MAP: Map<Short, EntityType>

		init {
			val nameMap = mutableMapOf<String, EntityType>()
			val idMap = mutableMapOf<Short, EntityType>()
			for (type in values()) {
				type.nameString?.let {
					nameMap[it.lowercase(Locale.ENGLISH)] = type
				}
				if (type.typeId > 0) {
					idMap[type.typeId] = type
				}
			}
			NAME_MAP = nameMap
			ID_MAP = idMap
		}

		@JvmStatic
		@Deprecated("Magic value")
		fun fromName(name: String?): EntityType? {
			return name?.let { NAME_MAP[it.lowercase(Locale.ENGLISH)] }
		}

		@JvmStatic
		@Deprecated("Magic value")
		fun fromId(id: Int): EntityType? {
			if (id > Short.MAX_VALUE) return null
			return ID_MAP[id.toShort()]
		}
	}

	@Deprecated("Magic value")
	fun getName(): String? = nameString

}

