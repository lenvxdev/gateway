package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.inventory.ItemStack
import dev.lenvx.gateway.player.Player

class PlayerSwapHandItemsEvent(
	player: Player,
	var mainHandItem: ItemStack?,
	var offHandItem: ItemStack?
) : PlayerEvent(player), Cancellable {

	override var isCancelled: Boolean = false

}

