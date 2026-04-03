package dev.lenvx.gateway.events.player

import dev.lenvx.gateway.network.protocol.packets.ServerboundResourcePackPacket.Action
import dev.lenvx.gateway.player.Player

class PlayerResourcePackStatusEvent(player: Player, val status: Action) : PlayerEvent(player)

