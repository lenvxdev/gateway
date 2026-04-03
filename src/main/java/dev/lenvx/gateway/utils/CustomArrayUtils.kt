package dev.lenvx.gateway.utils

object CustomArrayUtils {

	@JvmStatic
	fun longArrayToIntArray(numbers: LongArray): IntArray {
		return IntArray(numbers.size) { i -> numbers[i].toInt() }
	}

	@JvmStatic
	fun longArrayToByteArray(numbers: LongArray): ByteArray {
		return ByteArray(numbers.size) { i -> numbers[i].toByte() }
	}

}

