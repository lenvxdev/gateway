package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.events.Event
import dev.lenvx.gateway.network.ClientConnection
import net.kyori.adventure.text.Component

class PlayerLoginEvent(
	val connection: ClientConnection,
	private var cancelled: Boolean,
	var cancelReason: Component?
) : Event(), Cancellable {

	override var isCancelled: Boolean = false

}

