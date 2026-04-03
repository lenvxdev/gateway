package dev.lenvx.gateway.metrics

import java.util.concurrent.atomic.AtomicLong

class RuntimeMetrics {
    private val inboundPacketsCounter = AtomicLong(0)
    private val outboundPacketsCounter = AtomicLong(0)
    private val securityRejectCounter = AtomicLong(0)
    private val disconnectCounter = AtomicLong(0)

    fun incrementInboundPackets() {
        inboundPacketsCounter.incrementAndGet()
    }

    fun incrementOutboundPackets() {
        outboundPacketsCounter.incrementAndGet()
    }

    fun incrementSecurityRejects() {
        securityRejectCounter.incrementAndGet()
    }

    fun incrementDisconnects() {
        disconnectCounter.incrementAndGet()
    }

    fun snapshotAndReset(): Snapshot {
        return Snapshot(
            inboundPacketsCounter.getAndSet(0),
            outboundPacketsCounter.getAndSet(0),
            securityRejectCounter.getAndSet(0),
            disconnectCounter.getAndSet(0)
        )
    }

    data class Snapshot(
        val inboundPackets: Long,
        val outboundPackets: Long,
        val securityRejects: Long,
        val disconnects: Long
    )
}
