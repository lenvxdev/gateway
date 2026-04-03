package dev.lenvx.gateway.registry

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.network.ClientConnection
import dev.lenvx.gateway.network.protocol.packets.*
import net.kyori.adventure.key.Key
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.json.simple.parser.ParseException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.HashMap
import java.util.Objects

@Suppress("PatternValidation")
object PacketRegistry {

    private val ID_REGISTRY: MutableMap<NetworkPhase, Map<PacketBound, BiMap<Key, Int>>> = HashMap()
    private val CLASS_REGISTRY: BiMap<Class<out Packet>, PacketClassInfo> = HashBiMap.create()
    private val CLASS_ID_REGISTRY: MutableMap<Class<out Packet>, Int> = HashMap()

    init {
        val name = "reports/packets.json"
        val inputStream: InputStream? = Gateway::class.java.classLoader.getResourceAsStream(name)
        if (inputStream == null) {
            throw RuntimeException("Failed to load $name from jar!")
        }
        try {
            InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                val json = JSONParser().parse(reader) as JSONObject
                for (objKey in json.keys) {
                    val key = objKey as String
                    val networkPhase = NetworkPhase.fromName(key)
                    val mappings: MutableMap<PacketBound, BiMap<Key, Int>> = HashMap()
                    val jsonMappings = json[key] as JSONObject
                    for (objBoundKey in jsonMappings.keys) {
                        val boundKey = objBoundKey as String
                        val packetBound = PacketBound.fromName(boundKey)
                        val idMapping: BiMap<Key, Int> = HashBiMap.create()
                        val jsonIds = jsonMappings[boundKey] as JSONObject
                        for (objPacketKey in jsonIds.keys) {
                            val packetKey = objPacketKey as String
                            idMapping.put(Key.key(packetKey), ((jsonIds[packetKey] as JSONObject)["protocol_id"] as Number).toInt())
                        }
                        mappings[packetBound!!] = idMapping
                    }
                    ID_REGISTRY[networkPhase!!] = mappings
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        registerClass(PacketHandshakingIn::class.java, "minecraft:intention", NetworkPhase.HANDSHAKE, PacketBound.SERVERBOUND)

        registerClass(PacketStatusInRequest::class.java, "minecraft:status_request", NetworkPhase.STATUS, PacketBound.SERVERBOUND)
        registerClass(PacketStatusInPing::class.java, "minecraft:ping_request", NetworkPhase.STATUS, PacketBound.SERVERBOUND)

        registerClass(PacketStatusOutResponse::class.java, "minecraft:status_response", NetworkPhase.STATUS, PacketBound.CLIENTBOUND)
        registerClass(PacketStatusOutPong::class.java, "minecraft:pong_response", NetworkPhase.STATUS, PacketBound.CLIENTBOUND)

        registerClass(PacketLoginInLoginStart::class.java, "minecraft:hello", NetworkPhase.LOGIN, PacketBound.SERVERBOUND)
        registerClass(PacketLoginInPluginMessaging::class.java, "minecraft:custom_query_answer", NetworkPhase.LOGIN, PacketBound.SERVERBOUND)
        registerClass(ServerboundLoginAcknowledgedPacket::class.java, "minecraft:login_acknowledged", NetworkPhase.LOGIN, PacketBound.SERVERBOUND)

        registerClass(PacketLoginOutLoginSuccess::class.java, "minecraft:login_finished", NetworkPhase.LOGIN, PacketBound.CLIENTBOUND)
        registerClass(PacketLoginOutDisconnect::class.java, "minecraft:login_disconnect", NetworkPhase.LOGIN, PacketBound.CLIENTBOUND)
        registerClass(PacketLoginOutPluginMessaging::class.java, "minecraft:custom_query", NetworkPhase.LOGIN, PacketBound.CLIENTBOUND)

        registerClass(ServerboundFinishConfigurationPacket::class.java, "minecraft:finish_configuration", NetworkPhase.CONFIGURATION, PacketBound.SERVERBOUND)
        registerClass(PacketConfigurationInPluginMessaging::class.java, "minecraft:custom_payload", NetworkPhase.CONFIGURATION, PacketBound.SERVERBOUND)

        registerClass(ClientboundRegistryDataPacket::class.java, "minecraft:registry_data", NetworkPhase.CONFIGURATION, PacketBound.CLIENTBOUND)
        registerClass(ClientboundFinishConfigurationPacket::class.java, "minecraft:finish_configuration", NetworkPhase.CONFIGURATION, PacketBound.CLIENTBOUND)
        registerClass(PacketConfigurationOutPluginMessaging::class.java, "minecraft:custom_payload", NetworkPhase.CONFIGURATION, PacketBound.CLIENTBOUND)

        registerClass(PacketPlayInKeepAlive::class.java, "minecraft:keep_alive", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(ServerboundChatCommandPacket::class.java, "minecraft:chat_command", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInChat::class.java, "minecraft:chat", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInPosition::class.java, "minecraft:move_player_pos", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInPositionAndLook::class.java, "minecraft:move_player_pos_rot", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInRotation::class.java, "minecraft:move_player_rot", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInPluginMessaging::class.java, "minecraft:custom_payload", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInTabComplete::class.java, "minecraft:command_suggestion", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInHeldItemChange::class.java, "minecraft:set_carried_item", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(ServerboundResourcePackPacket::class.java, "minecraft:resource_pack", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInUseItem::class.java, "minecraft:use_item_on", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInBlockPlace::class.java, "minecraft:use_item", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInSetCreativeSlot::class.java, "minecraft:set_creative_mode_slot", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInWindowClick::class.java, "minecraft:container_click", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInCloseWindow::class.java, "minecraft:container_close", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInPickItem::class.java, "minecraft:pick_item", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInBlockDig::class.java, "minecraft:player_action", NetworkPhase.PLAY, PacketBound.SERVERBOUND)
        registerClass(PacketPlayInItemName::class.java, "minecraft:rename_item", NetworkPhase.PLAY, PacketBound.SERVERBOUND)

        registerClass(PacketPlayOutLogin::class.java, "minecraft:login", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutPositionAndLook::class.java, "minecraft:player_position", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutSpawnPosition::class.java, "minecraft:set_default_spawn_position", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundSystemChatPacket::class.java, "minecraft:system_chat", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutPlayerAbilities::class.java, "minecraft:player_abilities", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundLevelChunkWithLightPacket::class.java, "minecraft:level_chunk_with_light", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutUnloadChunk::class.java, "minecraft:forget_level_chunk", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutKeepAlive::class.java, "minecraft:keep_alive", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutGameStateChange::class.java, "minecraft:game_event", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutPlayerInfo::class.java, "minecraft:player_info_update", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutUpdateViewPosition::class.java, "minecraft:set_chunk_cache_center", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutDisconnect::class.java, "minecraft:disconnect", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutPluginMessaging::class.java, "minecraft:custom_payload", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutTabComplete::class.java, "minecraft:command_suggestions", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutDeclareCommands::class.java, "minecraft:commands", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutRespawn::class.java, "minecraft:respawn", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutEntityDestroy::class.java, "minecraft:remove_entities", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutEntityMetadata::class.java, "minecraft:set_entity_data", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutSpawnEntity::class.java, "minecraft:add_entity", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutHeldItemChange::class.java, "minecraft:set_carried_item", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutPlayerListHeaderFooter::class.java, "minecraft:tab_list", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundResourcePackPushPacket::class.java, "minecraft:resource_pack_push", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundSetTitlesAnimationPacket::class.java, "minecraft:set_titles_animation", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundSetTitleTextPacket::class.java, "minecraft:set_title_text", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundSetSubtitleTextPacket::class.java, "minecraft:set_subtitle_text", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundSetActionBarTextPacket::class.java, "minecraft:set_action_bar_text", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundClearTitlesPacket::class.java, "minecraft:clear_titles", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutBoss::class.java, "minecraft:boss_event", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutNamedSoundEffect::class.java, "minecraft:sound", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutStopSound::class.java, "minecraft:stop_sound", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutWindowItems::class.java, "minecraft:container_set_content", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutSetSlot::class.java, "minecraft:container_set_slot", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutOpenWindow::class.java, "minecraft:open_screen", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutCloseWindow::class.java, "minecraft:container_close", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(PacketPlayOutWindowData::class.java, "minecraft:container_set_data", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundChunkBatchFinishedPacket::class.java, "minecraft:chunk_batch_finished", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
        registerClass(ClientboundChunkBatchStartPacket::class.java, "minecraft:chunk_batch_start", NetworkPhase.PLAY, PacketBound.CLIENTBOUND)
    }

    private fun registerClass(packetClass: Class<out Packet>, key: String, networkPhase: NetworkPhase, packetBound: PacketBound) {
        val packetKey = Key.key(key)
        CLASS_REGISTRY[packetClass] = PacketClassInfo(packetKey, networkPhase, packetBound)
        val packetId = ID_REGISTRY[networkPhase]!![packetBound]!![packetKey]
            ?: throw IllegalStateException("Missing packet id mapping for $key ($networkPhase/$packetBound)")
        CLASS_ID_REGISTRY[packetClass] = packetId
    }

    @JvmStatic
    fun getPacketInfo(packetClass: Class<out Packet>): PacketClassInfo? {
        return CLASS_REGISTRY[packetClass]
    }

    @JvmStatic
    fun getPacketId(packetClass: Class<out Packet>): Int {
        return CLASS_ID_REGISTRY[packetClass]
            ?: throw IllegalArgumentException("Packet class is not registered: ${packetClass.name}")
    }

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <T : Packet> getPacketClass(packetId: Int, networkPhase: NetworkPhase, packetBound: PacketBound): Class<out T>? {
        val key = ID_REGISTRY[networkPhase]!![packetBound]!!.inverse()[packetId]
        if (key == null) {
            return null
        }
        return CLASS_REGISTRY.inverse()[PacketClassInfo(key, networkPhase, packetBound)] as Class<out T>?
    }

    enum class NetworkPhase(val phaseName: String, val clientState: ClientConnection.ClientState) {
        HANDSHAKE("handshake", ClientConnection.ClientState.HANDSHAKE),
        STATUS("status", ClientConnection.ClientState.STATUS),
        CONFIGURATION("configuration", ClientConnection.ClientState.CONFIGURATION),
        LOGIN("login", ClientConnection.ClientState.LOGIN),
        PLAY("play", ClientConnection.ClientState.PLAY);

        companion object {
            @JvmStatic
            fun fromName(name: String): NetworkPhase? {
                return values().find { it.phaseName == name }
            }

            @JvmStatic
            fun fromClientState(clientState: ClientConnection.ClientState): NetworkPhase? {
                return values().find { it.clientState == clientState }
            }
        }
    }

    enum class PacketBound(val boundName: String) {
        SERVERBOUND("serverbound"),
        CLIENTBOUND("clientbound");

        companion object {
            @JvmStatic
            fun fromName(name: String): PacketBound? {
                return values().find { it.boundName == name }
            }
        }
    }

    data class PacketClassInfo(val key: Key, val networkPhase: NetworkPhase, val packetBound: PacketBound)
}


