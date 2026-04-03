package dev.lenvx.gateway.events.inventory

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.inventory.InventoryView

class InventoryOpenEvent(view: InventoryView) : InventoryEvent(view, view.topInventory), Cancellable {

	private var cancelled: Boolean = false

	override var isCancelled: Boolean = false

}

