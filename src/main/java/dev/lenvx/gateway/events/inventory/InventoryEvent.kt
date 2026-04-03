package dev.lenvx.gateway.events.inventory

import dev.lenvx.gateway.events.Event
import dev.lenvx.gateway.inventory.Inventory
import dev.lenvx.gateway.inventory.InventoryView
import dev.lenvx.gateway.player.Player

open class InventoryEvent(
	val view: InventoryView,
	val clickedInventory: Inventory?
) : Event() {

	val player: Player get() = view.player
}

