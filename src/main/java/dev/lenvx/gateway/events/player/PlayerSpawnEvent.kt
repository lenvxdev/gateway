package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.player.Player

class PlayerSpawnEvent(player: Player, var spawnLocation: Location) : PlayerEvent(player)

