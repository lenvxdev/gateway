package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.player.Player

class PluginMessageEvent(
	player: Player,
	val channel: String,
	private val data: ByteArray
) : PlayerEvent(player) {

	fun getData(): ByteArray = data.copyOf()
}

