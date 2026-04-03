package dev.lenvx.gateway.world

import dev.lenvx.gateway.location.Location
import kotlin.math.floor

data class BlockPosition(val x: Int, val y: Int, val z: Int) {
	companion object {
		@JvmStatic
		fun from(location: Location): BlockPosition = 
			BlockPosition(floor(location.x).toInt(), floor(location.y).toInt(), floor(location.z).toInt())
	}
}

