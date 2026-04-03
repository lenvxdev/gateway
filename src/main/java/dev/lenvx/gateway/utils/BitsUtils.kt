package dev.lenvx.gateway.utils

import java.util.BitSet

object BitsUtils {

	@JvmStatic
	fun shiftAfter(bitset: BitSet, from: Int, shift: Int): BitSet {
		val subset = bitset.get(from, bitset.length())
		for (i in 0 until subset.length()) {
			bitset.set(from + shift + i, subset.get(i))
		}
		if (shift > 0) {
			for (i in 0 until shift) {
				bitset.set(from + i, false)
			}
		}
		return bitset
	}

	@JvmStatic
	fun toLongString(bitset: BitSet): String {
		return bitset.toLongArray().map { it.toULong().toString(2) }.toString()
	}

}

