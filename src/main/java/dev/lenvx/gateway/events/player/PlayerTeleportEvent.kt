package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.player.Player

class PlayerTeleportEvent(player: Player, from: Location, to: Location) : PlayerMoveEvent(player, from, to)

