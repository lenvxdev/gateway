package dev.lenvx.gateway.utils

import kotlin.math.abs


object NumberConversions {

	@JvmStatic
	fun floor(num: Double): Int {
		val floor = num.toInt()
		return if (floor.toDouble() == num) floor else floor - (java.lang.Double.doubleToRawLongBits(num).ushr(63)).toInt()
	}

	@JvmStatic
	fun ceil(num: Double): Int {
		val floor = num.toInt()
		return if (floor.toDouble() == num) floor else floor + (java.lang.Double.doubleToRawLongBits(num).inv().ushr(63)).toInt()
	}

	@JvmStatic
	fun round(num: Double): Int {
		return floor(num + 0.5)
	}

	@JvmStatic
	fun square(num: Double): Double {
		return num * num
	}

	@JvmStatic
	fun toInt(obj: Any?): Int {
		if (obj is Number) {
			return obj.toInt()
		}

		return try {
			obj.toString().toInt()
		} catch (e: Exception) {
			0
		}
	}

	@JvmStatic
	fun toFloat(obj: Any?): Float {
		if (obj is Number) {
			return obj.toFloat()
		}

		return try {
			obj.toString().toFloat()
		} catch (e: Exception) {
			0f
		}
	}

	@JvmStatic
	fun toDouble(obj: Any?): Double {
		if (obj is Number) {
			return obj.toDouble()
		}

		return try {
			obj.toString().toDouble()
		} catch (e: Exception) {
			0.0
		}
	}

	@JvmStatic
	fun toLong(obj: Any?): Long {
		if (obj is Number) {
			return obj.toLong()
		}

		return try {
			obj.toString().toLong()
		} catch (e: Exception) {
			0L
		}
	}

	@JvmStatic
	fun toShort(obj: Any?): Short {
		if (obj is Number) {
			return obj.toShort()
		}

		return try {
			obj.toString().toShort()
		} catch (e: Exception) {
			0
		}
	}

	@JvmStatic
	fun toByte(obj: Any?): Byte {
		if (obj is Number) {
			return obj.toByte()
		}

		return try {
			obj.toString().toByte()
		} catch (e: Exception) {
			0
		}
	}

	@JvmStatic
	fun isFinite(d: Double): Boolean {
		return abs(d) <= Double.MAX_VALUE
	}

	@JvmStatic
	fun isFinite(f: Float): Boolean {
		return abs(f) <= Float.MAX_VALUE
	}

	@JvmStatic
	fun checkFinite(d: Double, message: String) {
		if (!isFinite(d)) {
			throw IllegalArgumentException(message)
		}
	}

	@JvmStatic
	fun checkFinite(f: Float, message: String) {
		if (!isFinite(f)) {
			throw IllegalArgumentException(message)
		}
	}
}

