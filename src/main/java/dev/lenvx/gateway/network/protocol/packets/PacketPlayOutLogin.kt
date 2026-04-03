package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.registry.RegistryCustom
import dev.lenvx.gateway.utils.DataTypeIO
import dev.lenvx.gateway.utils.GameMode
import dev.lenvx.gateway.world.Environment
import dev.lenvx.gateway.world.World
import net.kyori.adventure.key.Key
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class PacketPlayOutLogin(
	val entityId: Int,
	val isHardcore: Boolean,
	val worlds: List<World>,
	val maxPlayers: Int,
	val viewDistance: Int,
	val simulationDistance: Int,
	val reducedDebugInfo: Boolean,
	val enableRespawnScreen: Boolean,
	val doLimitedCrafting: Boolean,
	val dimension: Environment,
	val world: World,
	val hashedSeed: Long,
	val gamemode: GameMode,
	val isDebug: Boolean,
	val isFlat: Boolean,
	val portalCooldown: Int,
	val seaLevel: Int,
	val enforcesSecureChat: Boolean
) : PacketOut() {

	@Throws(IOException::class)
	override fun serializePacket(): ByteArray {
		val buffer = ByteArrayOutputStream()
		val output = DataOutputStream(buffer)
		
		DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))
		output.writeInt(entityId)
		output.writeBoolean(isHardcore)
		DataTypeIO.writeVarInt(output, worlds.size)
		for (w in worlds) {
			DataTypeIO.writeString(output, Key.key(w.name).toString(), StandardCharsets.UTF_8)
		}
		DataTypeIO.writeVarInt(output, maxPlayers)
		DataTypeIO.writeVarInt(output, viewDistance)
		DataTypeIO.writeVarInt(output, simulationDistance)
		output.writeBoolean(reducedDebugInfo)
		output.writeBoolean(enableRespawnScreen)
		output.writeBoolean(doLimitedCrafting)
		DataTypeIO.writeVarInt(output, RegistryCustom.DIMENSION_TYPE.indexOf(world.environment.key))
		DataTypeIO.writeString(output, Key.key(world.name).toString(), StandardCharsets.UTF_8)
		output.writeLong(hashedSeed)
		output.writeByte(gamemode.id)
		output.writeByte(-1)
		output.writeBoolean(isDebug)
		output.writeBoolean(isFlat)
		output.writeBoolean(false)
		DataTypeIO.writeVarInt(output, portalCooldown)
		DataTypeIO.writeVarInt(output, seaLevel)
		output.writeBoolean(enforcesSecureChat)

		return buffer.toByteArray()
	}

}

