package dev.lenvx.gateway.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlidingWindowRateLimiterTest {

    @Test
    fun allowsEventsWithinLimit() {
        var now = 0L
        val limiter = SlidingWindowRateLimiter(windowMillis = 1000, maxEvents = 2) { now }

        assertTrue(limiter.tryAcquire())
        assertTrue(limiter.tryAcquire())
        assertFalse(limiter.tryAcquire())

        now = 1000L
        assertTrue(limiter.tryAcquire())
    }
}
