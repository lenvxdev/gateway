package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.network.ClientConnection
import dev.lenvx.gateway.registry.PacketRegistry

abstract class Packet {

	open fun getPacketState(): ClientConnection.ClientState {
		val info = PacketRegistry.getPacketInfo(javaClass)
			?: throw IllegalStateException("Unregistered packet class: ${javaClass.name}")
		return info.networkPhase.clientState
	}

}

