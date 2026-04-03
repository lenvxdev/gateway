package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.events.Event
import dev.lenvx.gateway.player.Player

open class PlayerEvent(val player: Player) : Event()

