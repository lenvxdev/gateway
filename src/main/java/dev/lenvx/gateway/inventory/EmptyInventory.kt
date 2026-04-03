package dev.lenvx.gateway.inventory

class EmptyInventory private constructor(inventoryHolder: InventoryHolder?) : AbstractInventory(0, inventoryHolder, InventoryType.CHEST, null, null) {

    companion object {
        @JvmStatic
        @Deprecated("")
        fun create(inventoryHolder: InventoryHolder?): EmptyInventory {
            return EmptyInventory(inventoryHolder)
        }
    }
}

