package dev.lenvx.gateway.inventory

import net.kyori.adventure.text.Component

class AnvilInventory(title: Component?, inventoryHolder: InventoryHolder?) : AbstractInventory(InventoryType.ANVIL.defaultSize, inventoryHolder, InventoryType.ANVIL, null, null), TitledInventory {

    override var title: Component = title ?: DEFAULT_TITLE
        private set

    fun setTitle(title: Component) {
        this.title = title
    }

    companion object {
        @JvmField
        val DEFAULT_TITLE: Component = Component.translatable("container.repair")
    }
}

