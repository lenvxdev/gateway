package dev.lenvx.gateway.inventory

import net.kyori.adventure.text.Component

class CustomInventory private constructor(
    title: Component?,
    size: Int,
    inventoryHolder: InventoryHolder?
) : AbstractInventory(size, inventoryHolder, InventoryType.CHEST, null, null), TitledInventory {

    override var title: Component = title ?: Component.empty()
        private set

    fun setTitle(title: Component?) {
        this.title = title ?: Component.empty()
    }

    companion object {
        @JvmStatic
        @Deprecated("")
        fun create(title: Component?, size: Int, inventoryHolder: InventoryHolder?): CustomInventory {
            require(!(size % 9 != 0 || size > 54 || size < 9)) { "size must be a multiple of 9 and within 9 - 54" }
            return CustomInventory(title, size, inventoryHolder)
        }
    }
}

