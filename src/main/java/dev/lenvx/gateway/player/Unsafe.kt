package dev.lenvx.gateway.player

import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.utils.GameMode

@Deprecated("Internal use only")
class Unsafe private constructor() {

	@Deprecated("Use Player.gamemode instead")
	fun a(player: Player, gamemode: GameMode) {
		player.gamemode = gamemode
	}

	@Deprecated("Use Player.entityId instead")
	@Suppress("DEPRECATION")
	fun a(player: Player, entityId: Int) {
		player.entityId = entityId
	}

	@Deprecated("Use Player.setLocation instead")
	fun a(player: Player, location: Location) {
		player.setLocation(location)
	}

	@Deprecated("Use Player.selectedSlot instead")
	fun a(player: Player, selectedSlot: Byte) {
		player.selectedSlot = selectedSlot
	}

}

