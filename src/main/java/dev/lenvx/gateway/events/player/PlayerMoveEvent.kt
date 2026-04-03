package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.player.Player


open class PlayerMoveEvent(
	player: Player,
	var from: Location,
	var to: Location
) : PlayerEvent(player), Cancellable {

	override var isCancelled: Boolean = false}

