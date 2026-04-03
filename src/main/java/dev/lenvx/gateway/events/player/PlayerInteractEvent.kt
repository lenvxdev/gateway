package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.inventory.EquipmentSlot
import dev.lenvx.gateway.inventory.ItemStack
import dev.lenvx.gateway.location.BlockFace
import dev.lenvx.gateway.player.Player
import dev.lenvx.gateway.world.BlockState

class PlayerInteractEvent(
	player: Player,
	val action: Action,
	val item: ItemStack?,
	val clickedBlock: BlockState?,
	val clickedFace: BlockFace?,
	val hand: EquipmentSlot?
) : PlayerEvent(player), Cancellable {

	enum class Action {
		LEFT_CLICK_AIR,
		LEFT_CLICK_BLOCK,
		PHYSICAL,
		RIGHT_CLICK_AIR,
		RIGHT_CLICK_BLOCK
	}

	override var isCancelled: Boolean = false

}

