package dev.lenvx.gateway.entity

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.entity.DataWatcher.WatchableField
import dev.lenvx.gateway.entity.DataWatcher.WatchableObjectType
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.utils.BungeeCordAdventureConversionUtils
import dev.lenvx.gateway.world.World
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent
import java.util.*

abstract class Entity(
	val type: EntityType,
	var entityId: Int,
	val uuid: UUID,
	var world: World,
	var x: Double,
	var y: Double,
	var z: Double,
	var yaw: Float,
	var pitch: Float
) : Sound.Emitter {

	constructor(type: EntityType, uuid: UUID, world: World, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) :
		this(type, Gateway.instance?.getNextEntityId() ?: -1, uuid, world, x, y, z, yaw, pitch)

	constructor(type: EntityType, world: World, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) :
		this(type, UUID.randomUUID(), world, x, y, z, yaw, pitch)

	constructor(type: EntityType, uuid: UUID, location: Location) :
		this(type, uuid, location.world, location.x, location.y, location.z, location.yaw, location.pitch)

	constructor(type: EntityType, location: Location) :
		this(type, UUID.randomUUID(), location)

	@WatchableField(MetadataIndex = 0, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x01)
	var isOnFire: Boolean = false

	@WatchableField(MetadataIndex = 0, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x02)
	var isCrouching: Boolean = false

	@WatchableField(MetadataIndex = 0, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x04)
	var isUnused: Boolean = false

	@WatchableField(MetadataIndex = 0, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x08)
	var isSprinting: Boolean = false

	@WatchableField(MetadataIndex = 0, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x10)
	var isSwimming: Boolean = false

	@WatchableField(MetadataIndex = 0, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x20)
	var isInvisible: Boolean = false

	@WatchableField(MetadataIndex = 0, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x40)
	var isGlowing: Boolean = false

	@WatchableField(MetadataIndex = 0, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x80)
	var isElytraFlying: Boolean = false

	@WatchableField(MetadataIndex = 1, WatchableObjectType = WatchableObjectType.VARINT)
	var air: Int = 300

	@WatchableField(MetadataIndex = 2, WatchableObjectType = WatchableObjectType.CHAT, IsOptional = true)
	var customName: Component? = null

	@WatchableField(MetadataIndex = 3, WatchableObjectType = WatchableObjectType.BOOLEAN)
	var isCustomNameVisible: Boolean = false

	@WatchableField(MetadataIndex = 4, WatchableObjectType = WatchableObjectType.BOOLEAN)
	var isSilent: Boolean = false

	@WatchableField(MetadataIndex = 5, WatchableObjectType = WatchableObjectType.BOOLEAN)
	var noGravity: Boolean = false

	@WatchableField(MetadataIndex = 6, WatchableObjectType = WatchableObjectType.POSE)
	var pose: Pose = Pose.STANDING

	@WatchableField(MetadataIndex = 7, WatchableObjectType = WatchableObjectType.VARINT)
	var frozenTicks: Int = 0

	open val location: Location
		get() = Location(world, x, y, z, yaw, pitch)

	open fun teleport(location: Location) {
		this.world = location.world
		this.x = location.x
		this.y = location.y
		this.z = location.z
		this.yaw = location.yaw
		this.pitch = location.pitch
	}

	fun setCustomName(name: String?) {
		this.customName = name?.let { LegacyComponentSerializer.legacySection().deserialize(it) }
	}

	@Deprecated("Use Adventure API")
	fun setCustomName(component: BaseComponent?) {
		this.customName = component?.let { BungeeCordAdventureConversionUtils.toComponent(it) }
	}

	@Deprecated("Use Adventure API")
	fun setCustomName(components: Array<BaseComponent>?) {
		this.customName = components?.let { BungeeCordAdventureConversionUtils.toComponent(*it) }
	}

	var hasGravity: Boolean
		get() = !noGravity
		set(value) {
			noGravity = !value
		}

	val uniqueId: UUID get() = uuid

	open fun isValid(): Boolean = world.getEntities().contains(this)

	open fun remove() {
		world.removeEntity(this)
	}

	open fun getDataWatcher(): DataWatcher? {
		return world.getDataWatcher(this)
	}

}



