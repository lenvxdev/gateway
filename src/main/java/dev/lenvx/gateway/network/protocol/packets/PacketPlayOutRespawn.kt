package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.registry.RegistryCustom
import dev.lenvx.gateway.utils.DataTypeIO
import dev.lenvx.gateway.utils.GameMode
import dev.lenvx.gateway.world.Environment
import dev.lenvx.gateway.world.World
import net.kyori.adventure.key.Key
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

class PacketPlayOutRespawn(
    val world: World,
    val hashedSeed: Long,
    val gamemode: GameMode,
    val isDebug: Boolean,
    val isFlat: Boolean,
    val copyMetaData: Boolean
) : PacketOut() {

    val dimension: Environment = world.environment

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))

        DataTypeIO.writeVarInt(output, RegistryCustom.DIMENSION_TYPE.indexOf(world.environment.key))
        DataTypeIO.writeString(output, Key.key(world.name).toString(), StandardCharsets.UTF_8)
        output.writeLong(hashedSeed)
        output.writeByte(gamemode.id)
        output.writeByte(gamemode.id)
        output.writeBoolean(isDebug)
        output.writeBoolean(isFlat)
        output.writeBoolean(copyMetaData)

        return buffer.toByteArray()
    }
}
