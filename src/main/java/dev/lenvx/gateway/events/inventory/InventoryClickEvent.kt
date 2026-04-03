package dev.lenvx.gateway.events.inventory

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.inventory.ClickType
import dev.lenvx.gateway.inventory.InventoryAction
import dev.lenvx.gateway.inventory.InventoryType
import dev.lenvx.gateway.inventory.InventoryView
import dev.lenvx.gateway.inventory.ItemStack

class InventoryClickEvent @JvmOverloads constructor(
	view: InventoryView,
	val slotType: InventoryType.SlotType,
	val rawSlot: Int,
	val click: ClickType,
	val action: InventoryAction,
	var hotbarKey: Int = -1
) : InventoryEvent(view, view.getInventory(rawSlot)), Cancellable {

	val whichSlot: Int = view.convertSlot(rawSlot)
	private var current: ItemStack? = null
	private var cancelled: Boolean = false

	var carriedItem: ItemStack?
		get() = view.carriedItem
		@Deprecated("Use view.carriedItem directly")
		set(value) {
			view.carriedItem = value
		}

	var currentItem: ItemStack?
		get() = if (slotType == InventoryType.SlotType.OUTSIDE) current else view.getItem(rawSlot)
		set(value) {
			if (slotType == InventoryType.SlotType.OUTSIDE) {
				current = value
			} else {
				view.setItem(rawSlot, value)
			}
		}

	override var isCancelled: Boolean = false

}

