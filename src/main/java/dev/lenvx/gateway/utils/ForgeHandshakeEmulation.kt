package dev.lenvx.gateway.utils

import java.nio.charset.StandardCharsets
import java.util.Locale

object ForgeHandshakeEmulation {

    const val REGISTER_CHANNEL = "minecraft:register"

    private val CHANNELS_TO_ADVERTISE = listOf(
        "fml:handshake",
        "fml:loginwrapper",
        "forge:handshake",
        "neoforge:main",
        "neoforge:network"
    )

    private val REGISTER_CHANNELS = setOf(
        "minecraft:register",
        "c:register"
    )

    private val KNOWN_HANDSHAKE_CHANNELS = setOf(
        "fml:handshake",
        "fml:loginwrapper",
        "forge:handshake",
        "neoforge:main",
        "neoforge:network"
    )

    @JvmStatic
    fun isLikelyForgeAddress(serverAddress: String?): Boolean {
        if (serverAddress.isNullOrEmpty()) {
            return false
        }
        val normalized = serverAddress.lowercase(Locale.ROOT)
        return normalized.contains("\u0000fml\u0000")
            || normalized.contains("\u0000forge\u0000")
            || normalized.contains("\u0000neoforge\u0000")
    }

    @JvmStatic
    fun advertisedChannelsPayload(): ByteArray {
        return CHANNELS_TO_ADVERTISE.joinToString("\u0000").toByteArray(StandardCharsets.UTF_8)
    }

    @JvmStatic
    fun responsePayload(channel: String, data: ByteArray): ByteArray? {
        val normalized = channel.trim().lowercase(Locale.ROOT)
        if (normalized in REGISTER_CHANNELS) {
            return advertisedChannelsPayload()
        }
        if (normalized in KNOWN_HANDSHAKE_CHANNELS) {
            return ByteArray(0)
        }
        if (normalized.contains("forge") || normalized.contains("fml")) {
            return ByteArray(0)
        }
        return null
    }
}
