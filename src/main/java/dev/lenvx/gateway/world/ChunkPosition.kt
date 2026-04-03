package dev.lenvx.gateway.world

import dev.lenvx.gateway.location.Location
import net.querz.mca.Chunk

data class ChunkPosition(val world: World, val chunkX: Int, val chunkZ: Int) {

	constructor(location: Location) : this(location.world, location.x.toInt() shr 4, location.z.toInt() shr 4)

	constructor(world: World, chunk: Chunk) : this(world, world.getChunkX(chunk), world.getChunkZ(chunk))

	val chunk: Chunk?
		get() = world.getChunkAt(chunkX, chunkZ)

}

