package dev.lenvx.gateway.player

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.entity.Entity
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.location.Vector
import dev.lenvx.gateway.network.ClientConnection
import dev.lenvx.gateway.network.protocol.packets.*
import dev.lenvx.gateway.world.ChunkPosition
import dev.lenvx.gateway.world.World
import net.querz.mca.Chunk
import java.io.IOException

class PlayerInteractManager {

	var player: Player? = null
		private set

	private var entities = mutableSetOf<Entity>()
	private var currentViewing = mutableMapOf<ChunkPosition, Chunk>()

	internal fun setPlayer(player: Player) {
		if (this.player == null) {
			this.player = player
		} else {
			throw RuntimeException("Player in PlayerInteractManager cannot be changed once created")
		}
	}

	@Throws(IOException::class)
	fun update() {
		val p = player ?: return
		if (p.clientConnection.clientState != ClientConnection.ClientState.PLAY) {
			return
		}

		val server = Gateway.instance ?: return
		val viewDistanceChunks = server.serverProperties.viewDistance
		val viewDistanceBlocks = viewDistanceChunks shl 4
		val location = p.location
		
		val entitiesInRange = p.world.getEntities()
			.filter { it.location.distanceSquared(location) < viewDistanceBlocks * viewDistanceBlocks }
			.toSet()

		for (entity in entitiesInRange) {
			if (!entities.contains(entity)) {
				val packet = PacketPlayOutSpawnEntity(entity.entityId, entity.uniqueId, entity.type, entity.x, entity.y, entity.z, entity.yaw, entity.pitch, entity.pitch, 0, Vector(0.0, 0.0, 0.0))
				p.clientConnection.sendPacket(packet)

				val meta = PacketPlayOutEntityMetadata(entity)
				p.clientConnection.sendPacket(meta)
			}
		}

		val removedIds = entities.filter { !entitiesInRange.contains(it) }.map { it.entityId }
		if (removedIds.isNotEmpty()) {
			val packet = PacketPlayOutEntityDestroy(*removedIds.toIntArray())
			p.clientConnection.sendPacket(packet)
		}

		entities = entitiesInRange.toMutableSet()

		val playerChunkX = location.x.toInt() shr 4
		val playerChunkZ = location.z.toInt() shr 4
		val world = location.world

		val chunksInRange = mutableMapOf<ChunkPosition, Chunk>()

		for (x in playerChunkX - viewDistanceChunks until playerChunkX + viewDistanceChunks) {
			for (z in playerChunkZ - viewDistanceChunks until playerChunkZ + viewDistanceChunks) {
				val chunk = world.getChunkAt(x, z)
				chunksInRange[ChunkPosition(world, x, z)] = chunk ?: World.EMPTY_CHUNK
			}
		}

		for (chunkPos in currentViewing.keys) {
			if (!chunksInRange.containsKey(chunkPos)) {
				p.clientConnection.sendPacket(PacketPlayOutUnloadChunk(chunkPos))
			}
		}

		var counter = 0
		p.clientConnection.sendPacket(ClientboundChunkBatchStartPacket())
		for ((chunkPos, chunk) in chunksInRange) {
			if (!currentViewing.containsKey(chunkPos)) {
				val realChunk = chunkPos.world.getChunkAt(chunkPos.chunkX, chunkPos.chunkZ)
				if (realChunk == null) {
					val chunkData = ClientboundLevelChunkWithLightPacket(chunkPos.chunkX, chunkPos.chunkZ, chunk, world.environment, emptyList(), emptyList())
					p.clientConnection.sendPacket(chunkData)
				} else {
					val blockChunk = world.lightEngineBlock.getBlockLightBitMask(chunkPos.chunkX, chunkPos.chunkZ)
					val skyChunk = if (world.hasSkyLight()) world.lightEngineSky?.getSkyLightBitMask(chunkPos.chunkX, chunkPos.chunkZ) else emptyList<Array<Byte>>()
					val chunkData = ClientboundLevelChunkWithLightPacket(chunkPos.chunkX, chunkPos.chunkZ, realChunk, world.environment, skyChunk ?: emptyList(), blockChunk)
					p.clientConnection.sendPacket(chunkData)
				}
				counter++
			}
		}
		p.clientConnection.sendPacket(ClientboundChunkBatchFinishedPacket(counter))

		currentViewing = chunksInRange
	}
}


