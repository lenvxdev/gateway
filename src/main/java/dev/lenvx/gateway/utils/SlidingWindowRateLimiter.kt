package dev.lenvx.gateway.utils

class SlidingWindowRateLimiter(
    private val windowMillis: Long,
    private val maxEvents: Int,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val events: ArrayDeque<Long> = ArrayDeque()

    @Synchronized
    fun tryAcquire(): Boolean {
        if (maxEvents <= 0) {
            return false
        }
        val now = nowMillis()
        while (events.isNotEmpty() && now - events.first() >= windowMillis) {
            events.removeFirst()
        }
        if (events.size >= maxEvents) {
            return false
        }
        events.addLast(now)
        return true
    }
}
