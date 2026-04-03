package dev.lenvx.gateway.events.inventory

import dev.lenvx.gateway.inventory.InventoryView

class InventoryCloseEvent(view: InventoryView) : InventoryEvent(view, view.topInventory)

