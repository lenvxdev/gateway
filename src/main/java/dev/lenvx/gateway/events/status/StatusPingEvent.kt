package dev.lenvx.gateway.events.status

import dev.lenvx.gateway.events.Event
import dev.lenvx.gateway.network.ClientConnection
import net.kyori.adventure.text.Component
import java.awt.image.BufferedImage

class StatusPingEvent(
	val connection: ClientConnection,
	var version: String,
	var protocol: Int,
	var motd: Component,
	var maxPlayers: Int,
	var playersOnline: Int,
	var favicon: BufferedImage?
) : Event()

