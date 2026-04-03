package dev.lenvx.gateway.world

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.entity.ArmorStand
import dev.lenvx.gateway.entity.DataWatcher
import dev.lenvx.gateway.entity.Entity
import dev.lenvx.gateway.entity.EntityType
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutEntityDestroy
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutEntityMetadata
import dev.lenvx.gateway.player.Player
import dev.lenvx.gateway.utils.SchematicConversionUtils
import net.querz.mca.Chunk
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.ListTag
import java.io.IOException
import java.lang.reflect.Field
import java.util.*

class World(val name: String, val width: Int, val length: Int, val environment: Environment) {

	companion object {
		@JvmField
		val HEIGHT_MAP = CompoundTag()
		@JvmField
		val EMPTY_CHUNK = Chunk.newChunk()

		init {
			HEIGHT_MAP.putLongArray("MOTION_BLOCKING",
				longArrayOf(1371773531765642314L, 1389823183635651148L, 1371738278539598925L,
						1389823183635388492L, 1353688558756731469L, 1389823114781694027L, 1317765589597723213L,
						1371773531899860042L, 1389823183635651149L, 1371773462911685197L, 1389823183635650636L,
						1353688626805119565L, 1371773531900123211L, 1335639250618849869L, 1371738278674077258L,
						1389823114781694028L, 1353723811310638154L, 1371738278674077259L, 1335674228429068364L,
						1335674228429067338L, 1335674228698027594L, 1317624576693539402L, 1335709481520370249L,
						1299610178184057417L, 1335638906349064264L, 1299574993811968586L, 1299574924958011464L,
						1299610178184056904L, 1299574924958011464L, 1299610109330100296L, 1299574924958011464L,
						1299574924823793736L, 1299574924958011465L, 1281525273222484040L, 1299574924958011464L,
						1281525273222484040L, 9548107335L))
			
			EMPTY_CHUNK.cleanupPalettesAndBlockStates()
			EMPTY_CHUNK.setHeightMaps(HEIGHT_MAP.clone())
			EMPTY_CHUNK.setBiomes(IntArray(1024))
			EMPTY_CHUNK.setTileEntities(ListTag(CompoundTag::class.java))
		}
	}

	val chunks: Array<Array<Chunk>> = Array((width shr 4) + 1) { x ->
		Array((length shr 4) + 1) { z ->
			Chunk.newChunk().apply {
				cleanupPalettesAndBlockStates()
				setHeightMaps(HEIGHT_MAP.clone())
				setBiomes(IntArray(1024))
				setTileEntities(ListTag(CompoundTag::class.java))
			}
		}
	}

	val lightEngineBlock = LightEngineBlock(this)
	var lightEngineSky: LightEngineSky? = if (environment.hasSkyLight()) LightEngineSky(this) else null
		private set

	private val entities: MutableMap<Entity, DataWatcher> = LinkedHashMap()

	fun hasSkyLight(): Boolean = lightEngineSky != null

	fun setBlock(x: Int, y: Int, z: Int, blockdata: String) {
		val chunkX = x shr 4
		val chunkZ = z shr 4
		if (chunkX < 0 || chunkZ < 0 || chunkX >= chunks.size || chunkZ >= chunks[0].size) return
		
		val chunk = chunks[chunkX][chunkZ]
		val block = SchematicConversionUtils.toBlockTag(blockdata)
		chunk.setBlockStateAt(x % 16, y, z % 16, block, false)
	}

	fun getBlock(blockPosition: BlockPosition): BlockState = getBlock(blockPosition.x, blockPosition.y, blockPosition.z)

	fun getBlock(x: Int, y: Int, z: Int): BlockState {
		val cx = x shr 4
		val cz = z shr 4
		if (cx < 0 || cz < 0 || cx >= chunks.size || cz >= chunks[0].size) {
			 return BlockState(CompoundTag().apply { putString("Name", "minecraft:air") })
		}
		val chunk = chunks[cx][cz]
		val tag = chunk.getBlockStateAt(x % 16, y, z % 16) ?: CompoundTag().apply { putString("Name", "minecraft:air") }
		return BlockState(tag)
	}

	fun setBlock(blockPosition: BlockPosition, state: BlockState) {
		setBlock(blockPosition.x, blockPosition.y, blockPosition.z, state)
	}

	fun setBlock(x: Int, y: Int, z: Int, state: BlockState) {
		val cx = x shr 4
		val cz = z shr 4
		if (cx < 0 || cz < 0 || cx >= chunks.size || cz >= chunks[0].size) return
		val chunk = chunks[cx][cz]
		chunk.setBlockStateAt(x % 16, y % 16, z % 16, state.toCompoundTag(), false)
	}

	fun getChunkAtWorldPos(x: Int, z: Int): Chunk? {
		val cx = x shr 4
		val cz = z shr 4
		if (cx < 0 || cz < 0 || cx >= chunks.size || cz >= chunks[0].size) return null
		return chunks[cx][cz]
	}

	fun getChunkAt(x: Int, z: Int): Chunk? {
		if (x < 0 || z < 0 || x >= chunks.size || z >= chunks[x].size) return null
		return chunks[x][z]
	}

	fun getChunkX(chunk: Chunk): Int {
		for (x in chunks.indices) {
			for (z in chunks[x].indices) {
				if (chunks[x][z] == chunk) return x
			}
		}
		return Int.MIN_VALUE
	}

	fun getChunkZ(chunk: Chunk): Int {
		for (x in chunks.indices) {
			for (z in chunks[x].indices) {
				if (chunks[x][z] == chunk) return z
			}
		}
		return Int.MIN_VALUE
	}

	fun getChunkXZ(chunk: Chunk): IntArray? {
		for (x in chunks.indices) {
			for (z in chunks[x].indices) {
				if (chunks[x][z] == chunk) return intArrayOf(x, z)
			}
		}
		return null
	}

	val chunkWidth: Int get() = (width shr 4) + 1
	val chunkLength: Int get() = (length shr 4) + 1

	fun getEntities(): Set<Entity> = Collections.unmodifiableSet(entities.keys)

	fun spawnEntity(type: EntityType, location: Location): Entity {
		if (location.world != this) throw IllegalArgumentException("Location not in world.")
		val entity = when (type) {
			EntityType.ARMOR_STAND -> ArmorStand(location)
			else -> throw UnsupportedOperationException("This EntityType cannot be summoned.")
		}
		entities[entity] = DataWatcher(entity)
		return entity
	}

	fun addEntity(entity: Entity): Entity {
		if (entity.world == this) {
			entities[entity] = DataWatcher(entity)
		} else {
			throw IllegalArgumentException("Location not in world.")
		}
		return entity
	}

	fun getPlayers(): List<Player> {
		return Gateway.instance?.players?.filter { it.world == this } ?: emptyList()
	}

	internal fun removeEntity(entity: Entity) {
		entities.remove(entity)
		val packet = PacketPlayOutEntityDestroy(entity.entityId)
		for (player in getPlayers()) {
			try {
				player.clientConnection.sendPacket(packet)
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
	}

	internal fun getDataWatcher(entity: Entity): DataWatcher? = entities[entity]

	@Throws(IllegalArgumentException::class, IllegalAccessException::class)
	fun update() {
		val iterator = entities.values.iterator()
		while (iterator.hasNext()) {
			val watcher = iterator.next()
			val entity = watcher.entity
			if (entity.world == this) {
					val updated = watcher.update() ?: emptyMap()
					if (updated.isNotEmpty()) {
					val packet = PacketPlayOutEntityMetadata(entity, false, *updated.keys.toTypedArray())
					for (player in getPlayers()) {
						try {
							player.clientConnection.sendPacket(packet)
						} catch (e: IOException) {
							e.printStackTrace()
						}
					}
				}
			} else {
				val packet = PacketPlayOutEntityDestroy(entity.entityId)
				for (player in getPlayers()) {
					try {
						player.clientConnection.sendPacket(packet)
					} catch (e: IOException) {
						e.printStackTrace()
					}
				}
				iterator.remove()
			}
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is World) return false
		return name == other.name
	}

	override fun hashCode(): Int = name.hashCode()
}


