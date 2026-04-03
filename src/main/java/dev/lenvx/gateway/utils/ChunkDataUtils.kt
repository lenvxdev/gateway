package dev.lenvx.gateway.utils

import net.querz.mca.Section
import kotlin.math.ceil

object ChunkDataUtils {

    @JvmStatic
    fun adjustBlockStateBits(newBits: Int, section: Section, dataVersion: Int) {
        val blockStates = section.blockStates
        val newBlockStates: LongArray = if (dataVersion < 2527) {
            if (newBits == blockStates.size / 64) blockStates else LongArray(newBits * 64)
        } else {
            val newLength = ceil(4096.0 / (64.0 / newBits)).toInt()
            if (newBits == blockStates.size / 64) blockStates else LongArray(newLength)
        }
        for (i in 0 until 4096) {
            section.setPaletteIndex(i, section.getPaletteIndex(i), newBlockStates)
        }
        section.blockStates = newBlockStates
    }
}
