package dev.lenvx.gateway.entity

import dev.lenvx.gateway.inventory.EquipmentSlot
import dev.lenvx.gateway.inventory.ItemStack


interface EntityEquipment {

	
	fun setItem(slot: EquipmentSlot, item: ItemStack?)

	
	fun getItem(slot: EquipmentSlot): ItemStack?

	
	var itemInMainHand: ItemStack?

	
	var itemInOffHand: ItemStack?

	
	var helmet: ItemStack?

	
	var chestplate: ItemStack?

	
	var leggings: ItemStack?

	
	var boots: ItemStack?

	
	var armorContents: Array<ItemStack?>

	
	fun clear()

	
	val holder: Entity?

}

