package dev.lenvx.gateway.entity

import java.lang.reflect.Field
import java.util.*

class DataWatcher(val entity: Entity) {

	private val values: MutableMap<Field, WatchableObject> = mutableMapOf()

	init {
		var clazz: Class<*>? = entity.javaClass
		while (clazz != null) {
			for (field in clazz.declaredFields) {
				val a = field.getAnnotation(WatchableField::class.java)
				if (a != null) {
					field.isAccessible = true
					try {
						values[field] = WatchableObject(
							field.get(entity),
							a.MetadataIndex,
							a.WatchableObjectType,
							a.IsOptional,
							a.IsBitmask,
							a.Bitmask
						)
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
			}
			clazz = clazz.superclass
		}
	}

	fun isValid(): Boolean = entity.isValid()

	@Synchronized
	@Throws(IllegalArgumentException::class, IllegalAccessException::class)
	fun update(): Map<Field, WatchableObject>? {
		if (!isValid()) return null

		val updated = mutableMapOf<Field, WatchableObject>()
		for ((field, watchableObj) in values) {
			field.isAccessible = true
			val newValue = field.get(entity)
			val oldValue = watchableObj.value
			if (newValue != oldValue) {
				watchableObj.value = newValue
				updated[field] = watchableObj
			}
		}
		return updated
	}

	val watchableObjects: Map<Field, WatchableObject>
		get() = Collections.unmodifiableMap(values)

	class WatchableObject @JvmOverloads constructor(
		var value: Any?,
		val index: Int,
		val type: WatchableObjectType,
		val isOptional: Boolean = false,
		val isBitmask: Boolean = false,
		val bitmask: Int = 0x00
	)

	@Retention(AnnotationRetention.RUNTIME)
	@Target(AnnotationTarget.FIELD)
	annotation class WatchableField(
		val MetadataIndex: Int,
		val WatchableObjectType: WatchableObjectType,
		val IsOptional: Boolean = false,
		val IsBitmask: Boolean = false,
		val Bitmask: Int = 0x00
	)

	enum class WatchableObjectType(val typeId: Int, val optionalTypeId: Int = -1) {
		BYTE(0),
		VARINT(1, 20),
		VARLONG(2, 17),
		FLOAT(3),
		STRING(4),
		CHAT(5, 6),
		SLOT(7),
		BOOLEAN(8),
		ROTATION(9),
		POSITION(10, 11),
		DIRECTION(12),
		UUID(-1, 13),
		BLOCKID(14, 15),
		NBT(16),
		PARTICLE(17),
		PARTICLES(18),
		VILLAGER_DATA(19),
		POSE(21),
		CAT_VARIANT(22),
		WOLF_VARIANT(23),
		FROG_VARIANT(24),
		GLOBAL_POSITION(-1, 25),
		PAINTING_VARIANT(26),
		SNIFFER_STATE(27),
		ARMADILLO_STATE(28),
		VECTOR3(29),
		QUATERNION(30)
	}
}

