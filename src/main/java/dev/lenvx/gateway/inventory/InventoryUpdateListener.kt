package dev.lenvx.gateway.inventory

fun interface InventoryUpdateListener {

    fun slotChanged(inventory: Inventory, slot: Int, oldItem: ItemStack?, newItem: ItemStack?)

}

