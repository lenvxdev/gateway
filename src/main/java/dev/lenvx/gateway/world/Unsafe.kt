package dev.lenvx.gateway.world

import dev.lenvx.gateway.entity.DataWatcher
import dev.lenvx.gateway.entity.Entity

@Deprecated("Internal use only")
class Unsafe internal constructor() {

	@Deprecated("Use World.removeEntity instead")
	fun a(world: World, entity: Entity) {
		world.removeEntity(entity)
	}

	@Deprecated("Use World.getDataWatcher instead")
	fun b(world: World, entity: Entity): DataWatcher? {
		return world.getDataWatcher(entity)
	}

}

