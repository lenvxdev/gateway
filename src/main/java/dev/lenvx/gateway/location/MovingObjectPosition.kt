package dev.lenvx.gateway.location

import dev.lenvx.gateway.entity.Entity

abstract class MovingObjectPosition protected constructor(val location: Vector) {

    fun distanceTo(entity: Entity): Double {
        val d0 = location.x - entity.x
        val d1 = location.y - entity.y
        val d2 = location.z - entity.z

        return d0 * d0 + d1 * d1 + d2 * d2
    }

    abstract val type: EnumMovingObjectType

    enum class EnumMovingObjectType {
        MISS, BLOCK, ENTITY;
    }
}

