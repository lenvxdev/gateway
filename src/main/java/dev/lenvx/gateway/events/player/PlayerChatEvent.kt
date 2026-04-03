package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.player.Player

class PlayerChatEvent(
	player: Player,
	var format: String,
	var message: String,
	private var cancelled: Boolean
) : PlayerEvent(player), Cancellable {

	override var isCancelled: Boolean = false

}

