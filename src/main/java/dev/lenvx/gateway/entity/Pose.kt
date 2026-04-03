package dev.lenvx.gateway.entity

enum class Pose(val id: Int) {
	
	STANDING(0),
	FALL_FLYING(1),
	SLEEPING(2),
	SWIMMING(3),
	SPIN_ATTACK(4),
	SNEAKING(5),
	DYING(6);

	companion object {
		private val VALUES = values()

		@JvmStatic
		fun fromId(id: Int): Pose? {
			for (pose in VALUES) {
				if (id == pose.id) {
					return pose
				}
			}
			return null
		}
	}

}

