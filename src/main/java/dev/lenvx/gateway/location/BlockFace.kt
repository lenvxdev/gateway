package dev.lenvx.gateway.location


enum class BlockFace(
    
    val modX: Int,
    
    val modY: Int,
    
    val modZ: Int
) {

    NORTH(0, 0, -1),
    EAST(1, 0, 0),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    UP(0, 1, 0),
    DOWN(0, -1, 0),
    NORTH_EAST(NORTH, EAST),
    NORTH_WEST(NORTH, WEST),
    SOUTH_EAST(SOUTH, EAST),
    SOUTH_WEST(SOUTH, WEST),
    WEST_NORTH_WEST(WEST, NORTH_WEST),
    NORTH_NORTH_WEST(NORTH, NORTH_WEST),
    NORTH_NORTH_EAST(NORTH, NORTH_EAST),
    EAST_NORTH_EAST(EAST, NORTH_EAST),
    EAST_SOUTH_EAST(EAST, SOUTH_EAST),
    SOUTH_SOUTH_EAST(SOUTH, SOUTH_EAST),
    SOUTH_SOUTH_WEST(SOUTH, SOUTH_WEST),
    WEST_SOUTH_WEST(WEST, SOUTH_WEST),
    SELF(0, 0, 0);

    constructor(face1: BlockFace, face2: BlockFace) : this(
        face1.modX + face2.modX,
        face1.modY + face2.modY,
        face1.modZ + face2.modZ
    )

    
    val direction: Vector
        get() {
            val direction = Vector(modX.toDouble(), modY.toDouble(), modZ.toDouble())
            if (modX != 0 || modY != 0 || modZ != 0) {
                direction.normalize()
            }
            return direction
        }

    
    val isCartesian: Boolean
        get() = when (this) {
            NORTH, SOUTH, EAST, WEST, UP, DOWN -> true
            else -> false
        }

    val oppositeFace: BlockFace
        get() = when (this) {
            NORTH -> SOUTH
            SOUTH -> NORTH
            EAST -> WEST
            WEST -> EAST
            UP -> DOWN
            DOWN -> UP
            NORTH_EAST -> SOUTH_WEST
            NORTH_WEST -> SOUTH_EAST
            SOUTH_EAST -> NORTH_WEST
            SOUTH_WEST -> NORTH_EAST
            WEST_NORTH_WEST -> EAST_SOUTH_EAST
            NORTH_NORTH_WEST -> SOUTH_SOUTH_EAST
            NORTH_NORTH_EAST -> SOUTH_SOUTH_WEST
            EAST_NORTH_EAST -> WEST_SOUTH_WEST
            EAST_SOUTH_EAST -> WEST_NORTH_WEST
            SOUTH_SOUTH_EAST -> NORTH_NORTH_WEST
            SOUTH_SOUTH_WEST -> NORTH_NORTH_EAST
            WEST_SOUTH_WEST -> EAST_NORTH_EAST
            SELF -> SELF
        }
}

