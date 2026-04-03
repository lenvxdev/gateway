package dev.lenvx.gateway.inventory

import net.kyori.adventure.text.Component

interface TitledInventory : Inventory {

    val title: Component

}

