package dev.lenvx.gateway.events.inventory

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.inventory.InventoryView
import dev.lenvx.gateway.inventory.ItemStack

class InventoryCreativeEvent(
	view: InventoryView,
	val slot: Int,
	newItem: ItemStack?
) : InventoryEvent(view, view.bottomInventory), Cancellable {
	private var _newItem: ItemStack? = newItem?.clone()

	var newItem: ItemStack?
		get() = _newItem?.clone()
		set(value) {
			_newItem = value?.clone()
		}

	override var isCancelled: Boolean = false

}

