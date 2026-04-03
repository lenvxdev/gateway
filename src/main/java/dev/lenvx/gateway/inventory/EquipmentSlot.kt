package dev.lenvx.gateway.inventory

enum class EquipmentSlot {
	
	MAINHAND,
	OFFHAND,
	HELMET,
	CHESTPLATE,
	LEGGINGS,
	BOOTS;

	fun isHandSlot(): Boolean {
		return when (this) {
			MAINHAND, OFFHAND -> true
			else -> false
		}
	}

	fun isArmorSlot(): Boolean {
		return when (this) {
			HELMET, CHESTPLATE, LEGGINGS, BOOTS -> true
			else -> false
		}
	}

}

