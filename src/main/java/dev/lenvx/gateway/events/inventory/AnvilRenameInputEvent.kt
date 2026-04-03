package dev.lenvx.gateway.events.inventory

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.inventory.InventoryView

class AnvilRenameInputEvent(view: InventoryView, var input: String) : InventoryEvent(view, view.topInventory), Cancellable {

	override var isCancelled: Boolean = false

}

