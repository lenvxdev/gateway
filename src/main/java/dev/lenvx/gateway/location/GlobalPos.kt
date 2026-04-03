package dev.lenvx.gateway.location

import dev.lenvx.gateway.world.BlockPosition
import dev.lenvx.gateway.world.World

data class GlobalPos(val world: World, val pos: BlockPosition) {

    companion object {
        @JvmStatic
        fun from(location: Location): GlobalPos {
            return GlobalPos(location.world, BlockPosition.from(location))
        }
    }
}

