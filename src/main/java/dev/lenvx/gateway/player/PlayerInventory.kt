package dev.lenvx.gateway.player

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableBiMap
import dev.lenvx.gateway.entity.EntityEquipment
import dev.lenvx.gateway.inventory.AbstractInventory
import dev.lenvx.gateway.inventory.EquipmentSlot
import dev.lenvx.gateway.inventory.InventoryType
import dev.lenvx.gateway.inventory.ItemStack
import java.util.*

class PlayerInventory(private val player: Player) : AbstractInventory(InventoryType.PLAYER.defaultSize, player, InventoryType.PLAYER, { i -> SLOT_MAPPING.getOrDefault(i, i) }, { i -> SLOT_MAPPING.inverse().getOrDefault(i, i) }), EntityEquipment {

	companion object {
		private val EQUIPMENT_SLOT_MAPPING: Map<EquipmentSlot, (PlayerInventory) -> Int>
		private val SLOT_MAPPING: BiMap<Int, Int>

		init {
			val equipmentSlotMapping = EnumMap<EquipmentSlot, (PlayerInventory) -> Int>(EquipmentSlot::class.java)
            equipmentSlotMapping[EquipmentSlot.MAINHAND] = { it.holder.selectedSlot.toInt() }
			equipmentSlotMapping[EquipmentSlot.OFFHAND] = { 40 }
			equipmentSlotMapping[EquipmentSlot.BOOTS] = { 36 }
			equipmentSlotMapping[EquipmentSlot.LEGGINGS] = { 37 }
			equipmentSlotMapping[EquipmentSlot.CHESTPLATE] = { 38 }
			equipmentSlotMapping[EquipmentSlot.HELMET] = { 39 }
			EQUIPMENT_SLOT_MAPPING = Collections.unmodifiableMap(equipmentSlotMapping)

			val slotMapping = HashBiMap.create<Int, Int>(41)
			for (i in 0 until 9) {
				slotMapping[i] = i + 36
			}
			for (i in 9 until 36) {
				slotMapping[i] = i
			}
			for (i in 36 until 40) {
				slotMapping[i] = i - 31
			}
			slotMapping[40] = 45
			SLOT_MAPPING = ImmutableBiMap.copyOf(slotMapping)
		}
	}

	init {
		viewers[player] = 0
	}

    override val holder: Player
        get() = player

	override fun setItem(slot: EquipmentSlot, item: ItemStack?) {
		setItem(EQUIPMENT_SLOT_MAPPING[slot]!!.invoke(this), item)
	}

	override fun getItem(slot: EquipmentSlot): ItemStack? {
		return getItem(EQUIPMENT_SLOT_MAPPING[slot]!!.invoke(this))
	}

	override var itemInMainHand: ItemStack?
		get() = getItem(EquipmentSlot.MAINHAND)
		set(value) = setItem(EquipmentSlot.MAINHAND, value)

	override var itemInOffHand: ItemStack?
		get() = getItem(EquipmentSlot.OFFHAND)
		set(value) = setItem(EquipmentSlot.OFFHAND, value)

	override var helmet: ItemStack?
		get() = getItem(EquipmentSlot.HELMET)
		set(value) = setItem(EquipmentSlot.HELMET, value)

	override var chestplate: ItemStack?
		get() = getItem(EquipmentSlot.CHESTPLATE)
		set(value) = setItem(EquipmentSlot.CHESTPLATE, value)

	override var leggings: ItemStack?
		get() = getItem(EquipmentSlot.LEGGINGS)
		set(value) = setItem(EquipmentSlot.LEGGINGS, value)

	override var boots: ItemStack?
		get() = getItem(EquipmentSlot.BOOTS)
		set(value) = setItem(EquipmentSlot.BOOTS, value)

	override var armorContents: Array<ItemStack?>
		get() = EquipmentSlot.values().filter { it.isArmorSlot() }.map { getItem(it) }.toTypedArray()
		set(value) {
			var i = 0
			for (equipmentSlot in EquipmentSlot.values()) {
				if (equipmentSlot.isArmorSlot()) {
					if (i < value.size) {
						setItem(equipmentSlot, value[i])
					}
					i++
				}
			}
		}

}

