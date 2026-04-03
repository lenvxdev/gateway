package dev.lenvx.gateway.entity

import dev.lenvx.gateway.inventory.EquipmentSlot
import dev.lenvx.gateway.inventory.ItemStack
import java.util.*

class StandardEntityEquipment(private val entity: Entity) : EntityEquipment {

	private val itemStacks = EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot::class.java)

	override fun setItem(slot: EquipmentSlot, item: ItemStack?) {
	}

	override fun getItem(slot: EquipmentSlot): ItemStack? {
		return itemStacks[slot]
	}

	override var itemInMainHand: ItemStack?
		get() = itemStacks[EquipmentSlot.MAINHAND]
		set(value) {}

	override var itemInOffHand: ItemStack?
		get() = getItem(EquipmentSlot.OFFHAND)
		set(value) {}

	override var helmet: ItemStack?
		get() = getItem(EquipmentSlot.HELMET)
		set(value) {}

	override var chestplate: ItemStack?
		get() = getItem(EquipmentSlot.CHESTPLATE)
		set(value) {}

	override var leggings: ItemStack?
		get() = getItem(EquipmentSlot.LEGGINGS)
		set(value) {}

	override var boots: ItemStack?
		get() = getItem(EquipmentSlot.BOOTS)
		set(value) {}

	override var armorContents: Array<ItemStack?>
		get() = itemStacks.values.toTypedArray()
		set(value) {
			if (value.size != 6) {
				throw IllegalArgumentException("items must have a length of 6")
			}
			val equipmentSlots = EquipmentSlot.values()
			for (i in equipmentSlots.indices) {
				setItem(equipmentSlots[i], value[i])
			}
		}

	override fun clear() {
		for (equipmentSlot in itemStacks.keys) {
			setItem(equipmentSlot, null)
		}
	}

	override val holder: Entity?
		get() = null

}


