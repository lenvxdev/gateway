package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.player.Player

class PlayerSelectedSlotChangeEvent(player: Player, var slot: Byte) : PlayerEvent(player), Cancellable {

	override var isCancelled: Boolean = false

}

