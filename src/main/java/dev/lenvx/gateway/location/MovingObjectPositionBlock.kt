package dev.lenvx.gateway.location

import dev.lenvx.gateway.world.BlockPosition

class MovingObjectPositionBlock : MovingObjectPosition {

    val direction: BlockFace
    val blockPos: BlockPosition
    private val miss: Boolean
    val isInside: Boolean
    val isWorldBorderHit: Boolean

    companion object {
        @JvmStatic
        fun miss(vec3d: Vector, direction: BlockFace, blockposition: BlockPosition): MovingObjectPositionBlock {
            return MovingObjectPositionBlock(true, vec3d, direction, blockposition, false, false)
        }
    }

    constructor(vec3d: Vector, direction: BlockFace, blockposition: BlockPosition, flag: Boolean) : this(
        false,
        vec3d,
        direction,
        blockposition,
        flag,
        false
    )

    constructor(
        vec3d: Vector,
        direction: BlockFace,
        blockposition: BlockPosition,
        flag: Boolean,
        flag1: Boolean
    ) : this(false, vec3d, direction, blockposition, flag, flag1)

    private constructor(
        flag: Boolean,
        vec3d: Vector,
        direction: BlockFace,
        blockposition: BlockPosition,
        flag1: Boolean,
        flag2: Boolean
    ) : super(vec3d) {
        this.miss = flag
        this.direction = direction
        this.blockPos = blockposition
        this.isInside = flag1
        this.isWorldBorderHit = flag2
    }

    fun withDirection(direction: BlockFace): MovingObjectPositionBlock {
        return MovingObjectPositionBlock(miss, location, direction, blockPos, isInside, isWorldBorderHit)
    }

    fun withPosition(blockposition: BlockPosition): MovingObjectPositionBlock {
        return MovingObjectPositionBlock(miss, location, direction, blockposition, isInside, isWorldBorderHit)
    }

    fun hitBorder(): MovingObjectPositionBlock {
        return MovingObjectPositionBlock(miss, location, direction, blockPos, isInside, true)
    }

    override val type: EnumMovingObjectType
        get() = if (miss) EnumMovingObjectType.MISS else EnumMovingObjectType.BLOCK
}

