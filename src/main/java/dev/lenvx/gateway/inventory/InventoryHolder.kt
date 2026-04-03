package dev.lenvx.gateway.inventory

import dev.lenvx.gateway.location.Location

interface InventoryHolder {

    val inventory: Inventory

    val holder: InventoryHolder?

    val location: Location?

}

