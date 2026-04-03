package dev.lenvx.gateway.entity

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.entity.DataWatcher.WatchableField
import dev.lenvx.gateway.entity.DataWatcher.WatchableObjectType
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.utils.Rotation3f
import dev.lenvx.gateway.world.World
import java.util.*

class ArmorStand(
	entityId: Int,
	uuid: UUID,
	world: World,
	x: Double,
	y: Double,
	z: Double,
	yaw: Float,
	pitch: Float
) : LivingEntity(EntityType.ARMOR_STAND, entityId, uuid, world, x, y, z, yaw, pitch) {

	constructor(uuid: UUID, world: World, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) :
		this(Gateway.instance?.getNextEntityId() ?: -1, uuid, world, x, y, z, yaw, pitch)

	constructor(world: World, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) :
		this(UUID.randomUUID(), world, x, y, z, yaw, pitch)

	constructor(uuid: UUID, location: Location) :
		this(uuid, location.world, location.x, location.y, location.z, location.yaw, location.pitch)

	constructor(location: Location) :
		this(UUID.randomUUID(), location)

	@WatchableField(MetadataIndex = 15, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x01)
	var isSmall: Boolean = false

	@WatchableField(MetadataIndex = 15, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x04)
	var showArms: Boolean = false

	@WatchableField(MetadataIndex = 15, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x08)
	var hasNoBasePlate: Boolean = false

	@WatchableField(MetadataIndex = 15, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x10)
	var isMarker: Boolean = false

	@WatchableField(MetadataIndex = 16, WatchableObjectType = WatchableObjectType.ROTATION)
	var headRotation: Rotation3f = Rotation3f(0.0, 0.0, 0.0)

	@WatchableField(MetadataIndex = 17, WatchableObjectType = WatchableObjectType.ROTATION)
	var bodyRotation: Rotation3f = Rotation3f(0.0, 0.0, 0.0)

	@WatchableField(MetadataIndex = 18, WatchableObjectType = WatchableObjectType.ROTATION)
	var leftArmRotation: Rotation3f = Rotation3f(-10.0, 0.0, -10.0)

	@WatchableField(MetadataIndex = 19, WatchableObjectType = WatchableObjectType.ROTATION)
	var rightArmRotation: Rotation3f = Rotation3f(-15.0, 0.0, 10.0)

	@WatchableField(MetadataIndex = 20, WatchableObjectType = WatchableObjectType.ROTATION)
	var leftLegRotation: Rotation3f = Rotation3f(-1.0, 0.0, -1.0)

	@WatchableField(MetadataIndex = 21, WatchableObjectType = WatchableObjectType.ROTATION)
	var rightLegRotation: Rotation3f = Rotation3f(1.0, 0.0, 1.0)

}


