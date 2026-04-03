package dev.lenvx.gateway.entity

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.entity.DataWatcher.WatchableField
import dev.lenvx.gateway.entity.DataWatcher.WatchableObjectType
import dev.lenvx.gateway.inventory.EquipmentSlot
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.world.BlockPosition
import dev.lenvx.gateway.world.World
import java.util.*

abstract class LivingEntity(
	type: EntityType,
	entityId: Int,
	uuid: UUID,
	world: World,
	x: Double,
	y: Double,
	z: Double,
	yaw: Float,
	pitch: Float
) : Entity(type, entityId, uuid, world, x, y, z, yaw, pitch) {

	constructor(type: EntityType, uuid: UUID, world: World, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) :
		this(type, Gateway.instance?.getNextEntityId() ?: -1, uuid, world, x, y, z, yaw, pitch)

	constructor(type: EntityType, world: World, x: Double, y: Double, z: Double, yaw: Float, pitch: Float) :
		this(type, UUID.randomUUID(), world, x, y, z, yaw, pitch)

	constructor(type: EntityType, uuid: UUID, location: Location) :
		this(type, uuid, location.world, location.x, location.y, location.z, location.yaw, location.pitch)

	constructor(type: EntityType, location: Location) :
		this(type, UUID.randomUUID(), location)

	@WatchableField(MetadataIndex = 8, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x01)
	var isHandActive: Boolean = false

	@WatchableField(MetadataIndex = 8, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x02)
	protected var activeHand: Boolean = false

	@WatchableField(MetadataIndex = 8, WatchableObjectType = WatchableObjectType.BYTE, IsBitmask = true, Bitmask = 0x04)
	var isInRiptideSpinAttack: Boolean = false

	@WatchableField(MetadataIndex = 9, WatchableObjectType = WatchableObjectType.FLOAT)
	var health: Float = 1.0f

	@WatchableField(MetadataIndex = 10, WatchableObjectType = WatchableObjectType.VARINT)
	var potionEffectColor: Int = 0

	@WatchableField(MetadataIndex = 11, WatchableObjectType = WatchableObjectType.BOOLEAN)
	var isPotionEffectAmbient: Boolean = false

	@WatchableField(MetadataIndex = 12, WatchableObjectType = WatchableObjectType.VARINT)
	var arrowsInEntity: Int = 0

	@WatchableField(MetadataIndex = 13, WatchableObjectType = WatchableObjectType.VARINT)
	var absorption: Int = 0

	@WatchableField(MetadataIndex = 14, WatchableObjectType = WatchableObjectType.POSITION, IsOptional = true)
	var sleepingLocation: BlockPosition? = null

	var activeHandSlot: EquipmentSlot
		get() = if (activeHand) EquipmentSlot.OFFHAND else EquipmentSlot.MAINHAND
		set(value) {
			activeHand = when (value) {
				EquipmentSlot.MAINHAND -> false
				EquipmentSlot.OFFHAND -> true
				else -> throw IllegalArgumentException("Invalid EquipmentSlot $value")
			}
		}

}



