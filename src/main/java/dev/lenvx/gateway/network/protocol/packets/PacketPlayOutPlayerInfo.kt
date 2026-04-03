package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import dev.lenvx.gateway.utils.GameMode
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.EnumSet
import java.util.Optional
import java.util.UUID

class PacketPlayOutPlayerInfo(
    val actions: EnumSet<PlayerInfoAction>,
    val uuid: UUID,
    val data: PlayerInfoData
) : PacketOut() {

    enum class PlayerInfoAction {
        ADD_PLAYER,
        INITIALIZE_CHAT,
        UPDATE_GAME_MODE,
        UPDATE_LISTED,
        UPDATE_LATENCY,
        UPDATE_DISPLAY_NAME
    }

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))

        DataTypeIO.writeEnumSet(output, actions, PlayerInfoAction::class.java)
        DataTypeIO.writeVarInt(output, 1)
        DataTypeIO.writeUUID(output, uuid)

        val infoData = data as PlayerInfoData.PlayerInfoDataAddPlayer
        for (action in actions) {
            when (action) {
                PlayerInfoAction.ADD_PLAYER -> {
                    DataTypeIO.writeString(output, infoData.name, StandardCharsets.UTF_8)
                    if (infoData.property.isPresent) {
                        DataTypeIO.writeVarInt(output, 1)
                        DataTypeIO.writeString(output, "textures", StandardCharsets.UTF_8)
                        DataTypeIO.writeString(output, infoData.property.get().skin, StandardCharsets.UTF_8)
                        output.writeBoolean(true)
                        DataTypeIO.writeString(output, infoData.property.get().signature, StandardCharsets.UTF_8)
                    } else {
                        DataTypeIO.writeVarInt(output, 0)
                    }
                }
                PlayerInfoAction.INITIALIZE_CHAT -> Unit
                PlayerInfoAction.UPDATE_GAME_MODE -> DataTypeIO.writeVarInt(output, infoData.gamemode.id)
                PlayerInfoAction.UPDATE_LISTED -> output.writeBoolean(infoData.listed)
                PlayerInfoAction.UPDATE_LATENCY -> DataTypeIO.writeVarInt(output, infoData.ping)
                PlayerInfoAction.UPDATE_DISPLAY_NAME -> {
                    if (infoData.displayNameJson.isPresent) {
                        output.writeBoolean(true)
                        DataTypeIO.writeString(output, infoData.displayNameJson.get(), StandardCharsets.UTF_8)
                    } else {
                        output.writeBoolean(false)
                    }
                }
            }
        }

        return buffer.toByteArray()
    }

    open class PlayerInfoData {
        class PlayerInfoDataAddPlayer(
            val name: String,
            val listed: Boolean,
            val property: Optional<PlayerSkinProperty>,
            val gamemode: GameMode,
            val ping: Int,
            val hasDisplayName: Boolean,
            val displayNameJson: Optional<String>
        ) : PlayerInfoData() {

            class PlayerSkinProperty(
                val skin: String,
                val signature: String
            )
        }
    }
}
