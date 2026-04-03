package dev.lenvx.gateway.sounds

import net.kyori.adventure.key.Key
import java.util.*

class SoundEffect private constructor(
    val sound: Key,
    val range: Float,
    val isNewSystem: Boolean
) {

    fun fixedRange(): Optional<Float> {
        return if (isNewSystem) Optional.of(range) else Optional.empty()
    }

    companion object {
        @JvmStatic
        fun createVariableRangeEvent(key: Key): SoundEffect {
            return SoundEffect(key, 16.0f, false)
        }

        @JvmStatic
        fun createFixedRangeEvent(key: Key, range: Float): SoundEffect {
            return SoundEffect(key, range, true)
        }
    }
}

