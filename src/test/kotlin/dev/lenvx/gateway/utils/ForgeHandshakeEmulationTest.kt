package dev.lenvx.gateway.utils

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ForgeHandshakeEmulationTest {

    @Test
    fun detectsLegacyForgeMarkersInHandshakeAddress() {
        assertTrue(ForgeHandshakeEmulation.isLikelyForgeAddress("example.org\u0000FML\u0000"))
        assertFalse(ForgeHandshakeEmulation.isLikelyForgeAddress("example.org"))
    }

    @Test
    fun returnsRegistrationPayloadForRegisterChannels() {
        val payload = ForgeHandshakeEmulation.responsePayload("minecraft:register", ByteArray(0))
        assertNotNull(payload)
        assertContentEquals(ForgeHandshakeEmulation.advertisedChannelsPayload(), payload)
    }

    @Test
    fun returnsAckPayloadForForgeHandshakeChannels() {
        val payload = ForgeHandshakeEmulation.responsePayload("neoforge:network", byteArrayOf(1, 2, 3))
        assertNotNull(payload)
        assertTrue(payload.isEmpty())
    }
}
