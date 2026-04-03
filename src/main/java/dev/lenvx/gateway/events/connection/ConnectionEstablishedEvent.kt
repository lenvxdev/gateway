package dev.lenvx.gateway.events.connection

import dev.lenvx.gateway.events.Event
import dev.lenvx.gateway.network.ClientConnection

class ConnectionEstablishedEvent(val connection: ClientConnection) : Event()

