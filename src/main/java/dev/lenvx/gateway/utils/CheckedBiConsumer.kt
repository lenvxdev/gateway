package dev.lenvx.gateway.utils

fun interface CheckedBiConsumer<T, U, TException : Throwable> {

	@Throws(Throwable::class)
	fun consume(t: T, u: U)

}

