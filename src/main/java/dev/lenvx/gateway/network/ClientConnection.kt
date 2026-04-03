package dev.lenvx.gateway.network

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.entity.EntityEquipment
import dev.lenvx.gateway.events.connection.ConnectionEstablishedEvent
import dev.lenvx.gateway.events.inventory.AnvilRenameInputEvent
import dev.lenvx.gateway.events.inventory.InventoryCloseEvent
import dev.lenvx.gateway.events.inventory.InventoryCreativeEvent
import dev.lenvx.gateway.events.player.*
import dev.lenvx.gateway.events.status.StatusPingEvent
import dev.lenvx.gateway.file.ServerProperties
import dev.lenvx.gateway.file.ServerProperties.ProtocolTranslationMode
import dev.lenvx.gateway.inventory.AnvilInventory
import dev.lenvx.gateway.inventory.Inventory
import dev.lenvx.gateway.inventory.ItemStack
import dev.lenvx.gateway.location.GlobalPos
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.network.protocol.packets.*
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutPlayerAbilities.PlayerAbilityFlags
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutPlayerInfo.PlayerInfoAction
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutPlayerInfo.PlayerInfoData
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutPlayerInfo.PlayerInfoData.PlayerInfoDataAddPlayer.PlayerSkinProperty
import dev.lenvx.gateway.network.protocol.packets.ServerboundResourcePackPacket.Action
import dev.lenvx.gateway.player.Player
import dev.lenvx.gateway.player.PlayerInteractManager
import dev.lenvx.gateway.player.PlayerInventory
import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.registry.RegistryCustom
import dev.lenvx.gateway.utils.*
import dev.lenvx.gateway.utils.MojangAPIUtils.SkinResponse
import dev.lenvx.gateway.world.World
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.io.*
import java.net.InetAddress
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.function.IntUnaryOperator
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.stream.Collectors

class ClientConnection(private val clientSocket: Socket) : Runnable {

    companion object {
        private val DEFAULT_HANDLER_NAMESPACE = Key.key("default")
        private val BRAND_ANNOUNCE_CHANNEL = Key.key("brand").toString()
        private val KEEP_ALIVE_TIMER = Timer("Gateway-KeepAlive", true)

        @JvmStatic
        fun isTranslationEnabled(mode: ProtocolTranslationMode): Boolean {
            return mode == ProtocolTranslationMode.BUILTIN
        }

        @JvmStatic
        fun isProtocolSupported(clientProtocol: Int, serverProtocol: Int, mode: ProtocolTranslationMode): Boolean {
            return clientProtocol == serverProtocol || isTranslationEnabled(mode)
        }
    }

    private val random = Random()
    private val packetSendLock: Lock = ReentrantLock()
    var channel: Channel? = null
        protected set
    var isRunning = false
        private set
    @Volatile
    var clientState: ClientState = ClientState.HANDSHAKE
        private set

    val lastPacketTimestamp = AtomicLong(-1)
    val lastKeepAlivePayLoad = AtomicLong(-1)
    val lastKeepAliveResponse = AtomicLong(-1)

    var player: Player? = null
        private set
    var keepAliveTask: TimerTask? = null
        private set
    var inetAddress: InetAddress = clientSocket.inetAddress
        private set
    var isReady = false
        private set
    private val inboundPacketLimiter: SlidingWindowRateLimiter by lazy {
        val maxPackets = Gateway.instance?.serverProperties?.maxInboundPacketsPerSecond ?: 400
        SlidingWindowRateLimiter(1000L, maxPackets)
    }

    fun sendPluginMessage(channel: String, data: ByteArray) {
        val packet = PacketPlayOutPluginMessaging(channel, data)
        sendPacket(packet)
    }

    fun sendPacket(packet: PacketOut) {
        packetSendLock.lock()
        try {
            if (channel?.writePacket(packet) == true) {
                lastPacketTimestamp.set(System.currentTimeMillis())
                Gateway.instance?.runtimeMetrics?.incrementOutboundPackets()
            }
        } finally {
            packetSendLock.unlock()
        }
    }

    fun disconnect(reason: Array<BaseComponent>) {
        disconnect(BungeeCordAdventureConversionUtils.toComponent(*reason))
    }

    fun disconnect(reason: Component) {
        Gateway.instance?.runtimeMetrics?.incrementDisconnects()
        try {
            val packet = PacketPlayOutDisconnect(reason)
            sendPacket(packet)
        } catch (ignored: IOException) {
        }
        try {
            val properties = Gateway.instance?.serverProperties
            val str = (if (properties?.isLogPlayerIPAddresses == true) inetAddress.hostName else "<ip address withheld>") + ":" + clientSocket.port
            Gateway.instance?.console?.sendMessage("[/$str] <-> Player disconnected with the reason " + PlainTextComponentSerializer.plainText().serialize(reason))
            clientSocket.close()
        } catch (ignored: IOException) {
        }
    }

    private fun disconnectDuringLogin(reason: Array<BaseComponent>) {
        disconnectDuringLogin(BungeeCordAdventureConversionUtils.toComponent(*reason))
    }

    private fun disconnectDuringLogin(reason: Component) {
        Gateway.instance?.runtimeMetrics?.incrementDisconnects()
        val properties = Gateway.instance?.serverProperties
        if (properties?.isReducedDebugInfo == false) {
            val str = (if (properties.isLogPlayerIPAddresses) inetAddress.hostName else "<ip address withheld>") + ":" + clientSocket.port
            Gateway.instance?.console?.sendMessage("[/$str] <-> Player disconnected with the reason " + PlainTextComponentSerializer.plainText().serialize(reason))
        }
        try {
            val packet = PacketLoginOutDisconnect(reason)
            sendPacket(packet)
        } catch (ignored: IOException) {
        }
        try {
            clientSocket.close()
        } catch (ignored: IOException) {
        }
    }

    private fun logSecurityEvent(code: String, detail: String) {
        val properties = Gateway.instance?.serverProperties
        val remote = if (properties?.isLogPlayerIPAddresses == true) inetAddress.hostAddress else "<ip address withheld>"
        Gateway.instance?.console?.sendMessage("[SECURITY] code=$code state=$clientState remote=$remote detail=$detail")
    }

    private fun rejectForSecurity(code: String, detail: String, duringLogin: Boolean, reason: String): Boolean {
        Gateway.instance?.runtimeMetrics?.incrementSecurityRejects()
        logSecurityEvent(code, detail)
        if (duringLogin) {
            disconnectDuringLogin(Component.text(reason))
        } else {
            disconnect(Component.text(reason))
        }
        return false
    }

    private fun allowInboundPacket(duringLogin: Boolean): Boolean {
        if (!inboundPacketLimiter.tryAcquire()) {
            return rejectForSecurity("packet_rate_limit", "Exceeded inbound packet rate limit", duringLogin, "Rate limit exceeded")
        }
        return true
    }

    private fun validatePlayPacket(packetIn: PacketIn): Boolean {
        val properties = Gateway.instance?.serverProperties ?: return true
        return when (packetIn) {
            is PacketPlayInChat ->
                if (!SecurityValidation.isChatMessageAllowed(packetIn.message, properties.maxChatMessageLength)) {
                    rejectForSecurity("chat_length", "Chat message exceeded max length ${properties.maxChatMessageLength}", false, "Chat message too long")
                } else {
                    true
                }
            is ServerboundChatCommandPacket ->
                if (!SecurityValidation.isCommandAllowed(packetIn.command, properties.maxCommandLength)) {
                    rejectForSecurity("command_length_or_chars", "Command exceeded length or contained control chars", false, "Invalid command payload")
                } else {
                    true
                }
            is PacketPlayInPluginMessaging ->
                if (!SecurityValidation.isPluginPayloadAllowed(packetIn.data.size, properties.maxPluginMessagePayloadBytes)) {
                    rejectForSecurity("plugin_payload", "Plugin payload size ${packetIn.data.size} exceeded ${properties.maxPluginMessagePayloadBytes}", false, "Plugin payload too large")
                } else {
                    true
                }
            else -> true
        }
    }

    private fun setChannel(input: DataInputStream, output: DataOutputStream) {
        this.channel = Channel(this, input, output).apply {
            addHandlerBefore(DEFAULT_HANDLER_NAMESPACE, object : ChannelPacketHandler() {
                override fun read(read: ChannelPacketRead): ChannelPacketRead? {
                    if (read.hasReadPacket()) {
                        return super.read(read)
                    }
                    try {
                        val dataInput = read.dataInput
                        val size = read.size
                        val packetId = read.packetId
                        val packetType = PacketRegistry.getPacketClass<PacketIn>(packetId, PacketRegistry.NetworkPhase.fromClientState(clientState)!!, PacketRegistry.PacketBound.SERVERBOUND)
                        if (packetType == null) {
                            dataInput.skipBytes(size - DataTypeIO.getVarIntLength(packetId))
                            return null
                        }
                        val constructors = packetType.constructors
                        val constructor = arraysStream(constructors).filter { each -> each.parameterCount > 0 && each.parameterTypes[0] == DataInputStream::class.java }.findFirst().orElse(null)
                        if (constructor == null) {
                            throw NoSuchMethodException("$packetType has no valid constructors!")
                        } else if (constructor.parameterCount == 1) {
                            read.readPacket = constructor.newInstance(dataInput) as PacketIn
                        } else if (constructor.parameterCount == 3) {
                            read.readPacket = constructor.newInstance(dataInput, size, packetId) as PacketIn
                        } else {
                            throw NoSuchMethodException("$packetType has no valid constructors!")
                        }
                        return super.read(read)
                    } catch (e: Exception) {
                        throw RuntimeException("Unable to read packet", e)
                    }
                }

                private fun <T> arraysStream(array: Array<T>): java.util.stream.Stream<T> {
                    return Arrays.stream(array)
                }
            })
        }
    }

    override fun run() {
        isRunning = true
        clientState = ClientState.HANDSHAKE
        try {
            clientSocket.keepAlive = true
            setChannel(DataInputStream(clientSocket.getInputStream()), DataOutputStream(clientSocket.getOutputStream()))

            Gateway.instance?.eventsManager?.callEvent(ConnectionEstablishedEvent(this))

            val handShakeSize = DataTypeIO.readVarInt(channel!!.input)
            if (handShakeSize == 0xFE) {
                val properties = Gateway.instance?.serverProperties
                clientState = ClientState.LEGACY
                channel!!.output.writeByte(255)
                val str = (if (properties?.isLogPlayerIPAddresses == true) inetAddress.hostName else "<ip address withheld>") + ":" + clientSocket.port
                Gateway.instance?.console?.sendMessage("[/$str] <-> Legacy Status has pinged")
                val p = Gateway.instance!!.serverProperties
                val event = Gateway.instance!!.eventsManager.callEvent(StatusPingEvent(this, p.versionString, p.protocol, p.motd, p.maxPlayers, Gateway.instance!!.players.size, p.favicon.orElse(null)))
                val response = Gateway.instance!!.buildLegacyPingResponse(event.version, event.motd, event.maxPlayers, event.playersOnline)
                val bytes = response.toByteArray(StandardCharsets.UTF_16BE)
                channel!!.output.writeShort(response.length)
                channel!!.output.write(bytes)

                channel!!.close()
                clientSocket.close()
                clientState = ClientState.DISCONNECTED
            }

            val handshake = channel!!.readPacket(handShakeSize) as PacketHandshakingIn

            val isBungeeCord = Gateway.instance?.serverProperties?.isBungeeCord ?: false
            val isBungeeGuard = Gateway.instance?.serverProperties?.isBungeeGuard ?: false
            val isVelocityModern = Gateway.instance?.serverProperties?.isVelocityModern ?: false
            val isForgeHandshakeLikely = ForgeHandshakeEmulation.isLikelyForgeAddress(handshake.serverAddress)
            val bungeeForwarding = handshake.serverAddress
            var bungeeUUID: UUID? = null
            var forwardedSkin: SkinResponse? = null

            try {
                when (handshake.handshakeType) {
                    PacketHandshakingIn.HandshakeType.STATUS -> {
                        clientState = ClientState.STATUS
                        while (clientSocket.isConnected) {
                            val packetIn = channel!!.readPacket()
                            Gateway.instance?.runtimeMetrics?.incrementInboundPackets()
                            if (!allowInboundPacket(true)) {
                                break
                            }
                            if (packetIn is PacketStatusInRequest) {
                                val properties = Gateway.instance?.serverProperties
                                val str = (if (properties?.isLogPlayerIPAddresses == true) inetAddress.hostName else "<ip address withheld>") + ":" + clientSocket.port
                                if (Gateway.instance?.serverProperties?.handshakeVerboseEnabled == true) {
                                    Gateway.instance?.console?.sendMessage("[/$str] <-> Handshake Status has pinged")
                                }
                                val p = Gateway.instance!!.serverProperties
                                val event = Gateway.instance!!.eventsManager.callEvent(StatusPingEvent(this, p.versionString, p.protocol, p.motd, p.maxPlayers, Gateway.instance!!.players.size, p.favicon.orElse(null)))
                                val response = PacketStatusOutResponse(Gateway.instance!!.buildServerListResponseJson(event.version, event.protocol, event.motd, event.maxPlayers, event.playersOnline, event.favicon))
                                sendPacket(response)
                            } else if (packetIn is PacketStatusInPing) {
                                val ping = packetIn
                                val pong = PacketStatusOutPong(ping.payload)
                                sendPacket(pong)
                                break
                            }
                        }
                    }
                    PacketHandshakingIn.HandshakeType.LOGIN, PacketHandshakingIn.HandshakeType.TRANSFER -> {
                        clientState = ClientState.LOGIN
                        val properties = Gateway.instance?.serverProperties
                        val protocolTranslationMode = properties?.protocolTranslationMode ?: ProtocolTranslationMode.BUILTIN

                        if (!isProtocolSupported(handshake.protocolVersion, Gateway.instance!!.SERVER_IMPLEMENTATION_PROTOCOL, protocolTranslationMode)) {
                            disconnectDuringLogin(Component.text("Unsupported protocol version. Native support is ${Gateway.instance!!.SERVER_IMPLEMENTATION_VERSION}. Set protocol-translation-mode=builtin to allow mixed-version logins."))
                            return
                        }

                        if (isBungeeCord || isBungeeGuard) {
                            try {
                                val data = bungeeForwarding.split("\u0000").toTypedArray()
                                if (data.size < 3) {
                                    throw IllegalStateException("Invalid bungee forwarding payload, expected at least 3 sections")
                                }

                                var index = 0
                                val host = data[index++]
                                var floodgate = ""
                                if (index < data.size && data[index].startsWith("^Floodgate^")) {
                                    floodgate = data[index++]
                                }
                                if (index + 1 >= data.size) {
                                    throw IllegalStateException("Invalid bungee forwarding payload, missing ip/uuid")
                                }

                                val clientIp = data[index++]
                                val bungee = data[index++]
                                val skinData = if (index < data.size) data[index] else ""

                                for (i in data.indices) {
                                    if (properties?.isReducedDebugInfo == false) {
                                        Gateway.instance?.console?.sendMessage("$i: ${data[i]}")
                                    }
                                }

                                if (properties?.isReducedDebugInfo == false) {
                                    Gateway.instance?.console?.sendMessage("Host: $host")
                                    Gateway.instance?.console?.sendMessage("Floodgate: $floodgate")
                                    Gateway.instance?.console?.sendMessage("clientIp: $clientIp")
                                    Gateway.instance?.console?.sendMessage("bungee: $bungee")
                                    Gateway.instance?.console?.sendMessage("skinData: $skinData")
                                }

                                bungeeUUID = UUID.fromString(bungee.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)".toRegex(), "$1-$2-$3-$4-$5"))
                                inetAddress = InetAddress.getByName(clientIp)

                                var bungeeGuardFound = false

                                if (skinData != "") {
                                    val skinJson = JSONParser().parse(skinData) as JSONArray
                                    for (obj in skinJson) {
                                        val property = obj as JSONObject
                                        if (property["name"].toString() == "textures") {
                                            val skin = property["value"].toString()
                                            val signature = property["signature"].toString()
                                            forwardedSkin = SkinResponse(skin, signature)
                                        } else if (isBungeeGuard && property["name"].toString() == "bungeeguard-token") {
                                            val token = property["value"].toString()
                                            bungeeGuardFound = Gateway.instance?.serverProperties?.forwardingSecrets?.contains(token) == true
                                        }
                                    }
                                }

                                if (isBungeeGuard && !bungeeGuardFound) {
                                    disconnectDuringLogin(Component.text("Invalid information forwarding"))
                                    return
                                }
                            } catch (e: Exception) {
                                if (properties?.isReducedDebugInfo == false) {
                                    val sw = StringWriter()
                                    val pw = PrintWriter(sw)
                                    e.printStackTrace(pw)
                                    Gateway.instance?.console?.sendMessage(sw.toString())
                                }
                                Gateway.instance?.console?.sendMessage("If you wish to use BungeeCord's IP forwarding, please enable that in your BungeeCord config.yml as well!")
                                disconnectDuringLogin(Component.text("Please connect from the proxy"))
                            }
                        }
                        val messageId = random.nextInt()
                        while (clientSocket.isConnected) {
                            val packetIn = channel!!.readPacket()
                            Gateway.instance?.runtimeMetrics?.incrementInboundPackets()
                            if (!allowInboundPacket(true)) {
                                break
                            }

                            if (packetIn is PacketLoginInLoginStart) {
                                val start = packetIn
                                val username = start.username

                                if (properties?.forgeHandshakeEmulationEnabled == true && isForgeHandshakeLikely && properties.isReducedDebugInfo == false) {
                                    Gateway.instance?.console?.sendMessage("Detected Forge/NeoForge handshake marker in incoming address.")
                                }

                                if (Gateway.instance?.serverProperties?.isVelocityModern == true) {
                                    val loginPluginRequest = PacketLoginOutPluginMessaging(messageId, ForwardingUtils.VELOCITY_FORWARDING_CHANNEL)
                                    sendPacket(loginPluginRequest)
                                    continue
                                }

                                var uuid = if (isBungeeCord || isBungeeGuard) bungeeUUID else start.uniqueId
                                if (uuid == null) {
                                    uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:$username").toByteArray(StandardCharsets.UTF_8))
                                }

                                if (properties?.enforceWhitelist == true && !properties.uuidWhitelisted(uuid)) {
                                    disconnectDuringLogin(Component.text("You are not whitelisted on the server"))
                                    break
                                }

                                val success = PacketLoginOutLoginSuccess(uuid, username)
                                sendPacket(success)

                                player = Player(this, username, uuid, Gateway.instance!!.getNextEntityId(), Gateway.instance!!.serverProperties.worldSpawn, PlayerInteractManager())
                                player!!.skinLayers = (0x01 or 0x02 or 0x04 or 0x08 or 0x10 or 0x20 or 0x40).toByte()
                            } else if (packetIn is PacketLoginInPluginMessaging) {
                                val response = packetIn
                                if (response.messageId != messageId) {
                                    disconnectDuringLogin(Component.text("Internal error, messageId did not match"))
                                    break
                                }
                                if (!response.data.isPresent) {
                                    disconnectDuringLogin(Component.text("Unknown login plugin response packet"))
                                    break
                                }
                                val responseData = response.data.get()
                                if (!SecurityValidation.isPluginPayloadAllowed(responseData.size, Gateway.instance!!.serverProperties.maxPluginMessagePayloadBytes)) {
                                    rejectForSecurity("login_plugin_payload", "Velocity forwarding response payload too large", true, "Invalid forwarding payload")
                                    break
                                }
                                if (!ForwardingUtils.validateVelocityModernResponse(responseData)) {
                                    disconnectDuringLogin(Component.text("Invalid playerinfo forwarding"))
                                    break
                                }
                                val data = ForwardingUtils.getVelocityDataFrom(responseData)
                                inetAddress = InetAddress.getByName(data.ipAddress)
                                forwardedSkin = data.skinResponse

                                val success = PacketLoginOutLoginSuccess(data.uuid, data.username)
                                sendPacket(success)

                                player = Player(this, data.username, data.uuid, Gateway.instance!!.getNextEntityId(), Gateway.instance!!.serverProperties.worldSpawn, PlayerInteractManager())
                                player!!.skinLayers = (0x01 or 0x02 or 0x04 or 0x08 or 0x10 or 0x20 or 0x40).toByte()
                            } else if (packetIn is ServerboundLoginAcknowledgedPacket) {
                                clientState = ClientState.CONFIGURATION
                                break
                            }
                        }

                        val event = Gateway.instance!!.eventsManager.callEvent(PlayerLoginEvent(this, false, Component.empty()))
                        if (event.isCancelled) {
                            disconnectDuringLogin(event.cancelReason ?: Component.text("Disconnected"))
                        }
                    }
                }
            } catch (ignored: Exception) {
                channel!!.close()
                clientSocket.close()
                clientState = ClientState.DISCONNECTED
            }

            if (clientState == ClientState.CONFIGURATION) {
                TimeUnit.MILLISECONDS.sleep(500)
                val properties = Gateway.instance!!.serverProperties

                for (registryCustom in RegistryCustom.getRegistries()) {
                    val registryDataPacket = ClientboundRegistryDataPacket(registryCustom)
                    sendPacket(registryDataPacket)
                }

                if (properties.forgeHandshakeEmulationEnabled) {
                    val registerPayload = ForgeHandshakeEmulation.advertisedChannelsPayload()
                    if (registerPayload.isNotEmpty()) {
                        val registerPacket = PacketConfigurationOutPluginMessaging(ForgeHandshakeEmulation.REGISTER_CHANNEL, registerPayload)
                        sendPacket(registerPacket)
                    }
                }

                val clientboundFinishConfigurationPacket = ClientboundFinishConfigurationPacket()
                sendPacket(clientboundFinishConfigurationPacket)

                var finishedConfiguration = false
                while (clientSocket.isConnected) {
                    val packetIn = channel!!.readPacket() ?: continue
                    Gateway.instance?.runtimeMetrics?.incrementInboundPackets()
                    if (!allowInboundPacket(true)) {
                        break
                    }
                    when (packetIn) {
                        is ServerboundFinishConfigurationPacket -> {
                            finishedConfiguration = true
                            break
                        }
                        is PacketConfigurationInPluginMessaging -> {
                            if (!SecurityValidation.isPluginPayloadAllowed(packetIn.data.size, properties.maxPluginMessagePayloadBytes)) {
                                rejectForSecurity("configuration_plugin_payload", "Configuration plugin payload too large", true, "Invalid configuration payload")
                                break
                            }
                            if (properties.forgeHandshakeEmulationEnabled) {
                                val responseData = ForgeHandshakeEmulation.responsePayload(packetIn.channel, packetIn.data)
                                if (responseData != null) {
                                    val responsePacket = PacketConfigurationOutPluginMessaging(packetIn.channel, responseData)
                                    sendPacket(responsePacket)
                                }
                            }
                        }
                    }
                }

                if (!finishedConfiguration) {
                    return
                }

                clientState = ClientState.PLAY
                val currentPlayer = player ?: throw IllegalStateException("Player not initialized")
                Gateway.instance!!.unsafe.a(currentPlayer)

                TimeUnit.MILLISECONDS.sleep(500)

                var worldSpawn = properties.worldSpawn

                val spawnEvent = Gateway.instance!!.eventsManager.callEvent(PlayerSpawnEvent(currentPlayer, worldSpawn))
                worldSpawn = spawnEvent.spawnLocation
                val world = worldSpawn.world

                val join = PacketPlayOutLogin(currentPlayer.entityId, false, Gateway.instance!!.worlds, properties.maxPlayers, 8, 8, properties.isReducedDebugInfo, true, false, world.environment, world, 0, properties.defaultGamemode, false, true, 0, 0, false)
                sendPacket(join)
                Gateway.instance!!.unsafe.a(currentPlayer, properties.defaultGamemode)

                val brandOut = ByteArrayOutputStream()
                DataTypeIO.writeString(DataOutputStream(brandOut), properties.serverModName, StandardCharsets.UTF_8)
                sendPluginMessage(BRAND_ANNOUNCE_CHANNEL, brandOut.toByteArray())
                if (properties.forgeHandshakeEmulationEnabled) {
                    val registerPayload = ForgeHandshakeEmulation.advertisedChannelsPayload()
                    if (registerPayload.isNotEmpty()) {
                        sendPluginMessage(ForgeHandshakeEmulation.REGISTER_CHANNEL, registerPayload)
                    }
                }

                val skinResponse = if ((isVelocityModern || isBungeeGuard || isBungeeCord) && forwardedSkin != null) forwardedSkin else MojangAPIUtils.getSkinFromMojangServer(currentPlayer.name)
                val skin = skinResponse?.let { PlayerSkinProperty(it.skin, it.signature) }
                val info = PacketPlayOutPlayerInfo(EnumSet.of(PlayerInfoAction.ADD_PLAYER, PlayerInfoAction.UPDATE_GAME_MODE, PlayerInfoAction.UPDATE_LISTED, PlayerInfoAction.UPDATE_LATENCY, PlayerInfoAction.UPDATE_DISPLAY_NAME), currentPlayer.uniqueId, PlayerInfoData.PlayerInfoDataAddPlayer(currentPlayer.name, true, Optional.ofNullable(skin), properties.defaultGamemode, 0, false, Optional.empty()))
                sendPacket(info)

                val flags = HashSet<PlayerAbilityFlags>()
                if (properties.isAllowFlight) {
                    flags.add(PlayerAbilityFlags.FLY)
                }
                if (currentPlayer.gamemode == GameMode.CREATIVE) {
                    flags.add(PlayerAbilityFlags.CREATIVE)
                }
                val abilities = PacketPlayOutPlayerAbilities(0.05f, 0.1f, *flags.toTypedArray())
                sendPacket(abilities)

                val str = (if (properties.isLogPlayerIPAddresses) inetAddress.hostName else "<ip address withheld>") + ":" + clientSocket.port + "|" + currentPlayer.name + "(" + currentPlayer.uniqueId + ")"
                Gateway.instance!!.console.sendMessage("[/$str] <-> Player had connected to the Gateway server!")

                val gameEvent = PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.GameStateChangeEvent.LEVEL_CHUNKS_LOAD_START, 0f)
                sendPacket(gameEvent)

                currentPlayer.playerInteractManager.update()

                val declare = DeclareCommands.getDeclareCommandsPacket(currentPlayer)
                if (declare != null) {
                    sendPacket(declare)
                }

                val spawnPos = PacketPlayOutSpawnPosition(GlobalPos.from(worldSpawn), worldSpawn.yaw, worldSpawn.pitch)
                sendPacket(spawnPos)

                val positionLook = PacketPlayOutPositionAndLook(worldSpawn.x, worldSpawn.y, worldSpawn.z, worldSpawn.yaw, worldSpawn.pitch, 1)
                Gateway.instance!!.unsafe.a(currentPlayer, Location(world, worldSpawn.x, worldSpawn.y, worldSpawn.z, worldSpawn.yaw, worldSpawn.pitch))
                sendPacket(positionLook)

                currentPlayer.getDataWatcher()?.update()
                val show = PacketPlayOutEntityMetadata(currentPlayer, false, Player::class.java.getDeclaredField("skinLayers"))
                sendPacket(show)

                Gateway.instance!!.eventsManager.callEvent(PlayerJoinEvent(currentPlayer))

                if (properties.isAllowFlight) {
                    val state = PacketPlayOutGameStateChange(PacketPlayOutGameStateChange.GameStateChangeEvent.CHANGE_GAME_MODE, currentPlayer.gamemode.id.toFloat())
                    sendPacket(state)
                }

                val resourcePackLink = properties.resourcePackLink ?: ""
                val resourcePackSha1 = properties.resourcePackSHA1 ?: ""
                if (resourcePackLink.isNotEmpty()) {
                    if (resourcePackSha1.isNotEmpty()) {
                        currentPlayer.setResourcePack(resourcePackLink, resourcePackSha1, properties.resourcePackRequired, properties.resourcePackPrompt)
                    } else {
                        Gateway.instance!!.console.sendMessage("ResourcePacks require SHA1s")
                    }
                }

                currentPlayer.sendPlayerListHeaderAndFooter(properties.tabHeader, properties.tabFooter)

                isReady = true

                keepAliveTask = object : TimerTask() {
                    override fun run() {
                        if (clientState != ClientState.PLAY || !isReady) {
                            cancel()
                        }

                        val now = System.currentTimeMillis()
                        val keepAlive = PacketPlayOutKeepAlive(now)
                        try {
                            sendPacket(keepAlive)
                            lastKeepAlivePayLoad.set(now)
                        } catch (e: IOException) {
                            cancel()
                        }
                    }
                }
                KEEP_ALIVE_TIMER.scheduleAtFixedRate(keepAliveTask, 0, 10000)

                while (clientSocket.isConnected) {
                    try {
                        val packetIn = channel!!.readPacket() ?: continue
                        Gateway.instance?.runtimeMetrics?.incrementInboundPackets()
                        if (!allowInboundPacket(false) || !validatePlayPacket(packetIn)) {
                            break
                        }

                        when (packetIn) {
                            is PacketPlayInPositionAndLook -> {
                                val pos = packetIn
                                val from = player!!.location
                                val to = Location(player!!.world, pos.x, pos.y, pos.z, pos.yaw, pos.pitch)
                                if (from != to) {
                                    val event = Gateway.instance!!.eventsManager.callEvent(PlayerMoveEvent(player!!, from, to))
                                    processMoveEvent(event, to)
                                }
                            }
                            is PacketPlayInPosition -> {
                                val pos = packetIn
                                val from = player!!.location
                                val to = Location(player!!.world, pos.x, pos.y, pos.z, from.yaw, from.pitch)
                                if (from != to) {
                                    val event = Gateway.instance!!.eventsManager.callEvent(PlayerMoveEvent(player!!, from, to))
                                    processMoveEvent(event, to)
                                }
                            }
                            is PacketPlayInRotation -> {
                                val pos = packetIn
                                val from = player!!.location
                                val to = Location(player!!.world, from.x, from.y, from.z, pos.yaw, pos.pitch)
                                if (from != to) {
                                    val event = Gateway.instance!!.eventsManager.callEvent(PlayerMoveEvent(player!!, from, to))
                                    processMoveEvent(event, to)
                                }
                            }
                            is PacketPlayInKeepAlive -> {
                                if (packetIn.payload == lastKeepAlivePayLoad.get()) {
                                    lastKeepAliveResponse.set(System.currentTimeMillis())
                                } else {
                                    disconnect(Component.text("Bad Keepalive Payload"))
                                    break
                                }
                            }
                            is PacketPlayInTabComplete -> {
                                val command = CustomStringUtils.splitStringToArgs(packetIn.text.substring(1))
                                val matches = Gateway.instance!!.pluginManager.getTabOptions(player!!, command).stream().map { PacketPlayOutTabComplete.TabCompleteMatches(it) }.collect(Collectors.toList()).toTypedArray()
                                val start = CustomStringUtils.getIndexOfArg(packetIn.text, command.size - 1) + 1
                                val length = command[command.size - 1].length
                                val response = PacketPlayOutTabComplete(packetIn.id, start, length, *matches)
                                sendPacket(response)
                            }
                            is PacketPlayInChat -> {
                                player!!.chat(packetIn.message, true, packetIn.signature, packetIn.time)
                            }
                            is ServerboundChatCommandPacket -> {
                                Gateway.instance!!.dispatchCommand(player!!, "/" + packetIn.command)
                            }
                            is PacketPlayInHeldItemChange -> {
                                val event = Gateway.instance!!.eventsManager.callEvent(PlayerSelectedSlotChangeEvent(player!!, packetIn.slot.toByte()))
                                if (event.isCancelled) {
                                    sendPacket(PacketPlayOutHeldItemChange(player!!.selectedSlot))
                                } else if (packetIn.slot.toByte() != event.slot) {
                                    sendPacket(PacketPlayOutHeldItemChange(event.slot))
                                    Gateway.instance!!.unsafe.a(player!!, event.slot)
                                } else {
                                    Gateway.instance!!.unsafe.a(player!!, event.slot)
                                }
                            }
                            is ServerboundResourcePackPacket -> {
                                Gateway.instance!!.eventsManager.callEvent(PlayerResourcePackStatusEvent(player!!, packetIn.action))
                                if (packetIn.action == Action.DECLINED && properties.resourcePackRequired) {
                                    player!!.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"))
                                }
                            }
                            is PacketPlayInPluginMessaging -> {
                                Gateway.instance!!.eventsManager.callEvent(PluginMessageEvent(player!!, packetIn.channel, packetIn.data))
                                if (properties.forgeHandshakeEmulationEnabled) {
                                    val responseData = ForgeHandshakeEmulation.responsePayload(packetIn.channel, packetIn.data)
                                    if (responseData != null) {
                                        sendPluginMessage(packetIn.channel, responseData)
                                    }
                                }
                            }
                            is PacketPlayInBlockPlace -> {
                                Gateway.instance!!.eventsManager.callEvent(PlayerInteractEvent(player!!, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, player!!.equipment.getItem(packetIn.hand), null, null, packetIn.hand))
                            }
                            is PacketPlayInUseItem -> {
                                val block = player!!.world.getBlock(packetIn.blockHit.blockPos)
                                Gateway.instance!!.eventsManager.callEvent(PlayerInteractEvent(player!!, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, player!!.equipment.getItem(packetIn.hand), block, packetIn.blockHit.direction, packetIn.hand))
                            }
                            is PacketPlayInSetCreativeSlot -> {
                                val inverseSlotConvertor = player!!.inventory.getUnsafe().b() ?: IntUnaryOperator.identity()
                                val event = Gateway.instance!!.eventsManager.callEvent(InventoryCreativeEvent(player!!.inventoryView, inverseSlotConvertor.applyAsInt(packetIn.slotNumber), packetIn.itemStack))
                                if (event.isCancelled) {
                                    player!!.updateInventory()
                                } else {
                                    player!!.inventory.setItem(event.slot, event.newItem)
                                }
                            }
                            is PacketPlayInWindowClick -> {
                                try {
                                    InventoryClickUtils.handle(player!!, packetIn)
                                } catch (e: Throwable) {
                                    e.printStackTrace()
                                }
                            }
                            is PacketPlayInCloseWindow -> {
                                val inventory = player!!.inventoryView.topInventory
                                if (inventory != null) {
                                    val id = inventory.getUnsafe().c()?.get(player!!)
                                    if (id != null) {
                                        Gateway.instance!!.eventsManager.callEvent(InventoryCloseEvent(player!!.inventoryView))
                                        player!!.inventoryView.unsafe.a(null, Component.empty())
                                        (inventory.getUnsafe().c() as? MutableMap<Player, Int>)?.remove(player!!)
                                    }
                                }
                            }
                            is PacketPlayInBlockDig -> {
                                when (packetIn.action) {
                                    PacketPlayInBlockDig.PlayerDigAction.SWAP_ITEM_WITH_OFFHAND -> {
                                        val equipment = player!!.equipment
                                        val event = Gateway.instance!!.eventsManager.callEvent(PlayerSwapHandItemsEvent(player!!, equipment.itemInOffHand, equipment.itemInMainHand))
                                        if (!event.isCancelled) {
                                            equipment.itemInMainHand = event.mainHandItem
                                            equipment.itemInOffHand = event.offHandItem
                                        }
                                    }
                                    else -> {}
                                }
                            }
                            is PacketPlayInPickItem -> {
                                val inventory = player!!.inventory
                                val inverseSlotConvertor = inventory.getUnsafe().b() ?: IntUnaryOperator.identity()
                                val slot = inverseSlotConvertor.applyAsInt(packetIn.slot)
                                var i = player!!.selectedSlot.toInt()
                                var selectedSlot: Byte = -1
                                var firstRun = true
                                while (selectedSlot < 0 || (!firstRun && i == player!!.selectedSlot.toInt())) {
                                    val itemStack = inventory.getItem(i)
                                    if (itemStack == null) {
                                        selectedSlot = i.toByte()
                                        break
                                    }
                                    if (++i >= 9) i = 0
                                    firstRun = false
                                }
                                if (selectedSlot < 0) selectedSlot = player!!.selectedSlot
                                val leavingHotbar = inventory.getItem(selectedSlot.toInt())
                                inventory.setItem(selectedSlot.toInt(), inventory.getItem(slot))
                                inventory.setItem(slot, leavingHotbar)
                                player!!.selectedSlot = selectedSlot
                            }
                            is PacketPlayInItemName -> {
                                if (player!!.inventoryView.topInventory is AnvilInventory) {
                                    val event = Gateway.instance!!.eventsManager.callEvent(AnvilRenameInputEvent(player!!.inventoryView, packetIn.name))
                                    if (!event.isCancelled) {
                                        val anvilInventory = player!!.inventoryView.topInventory as AnvilInventory
                                        val result = anvilInventory.getItem(2)
                                        result?.displayName(LegacyComponentSerializer.legacySection().deserialize(event.input))
                                    }
                                }
                            }
                        }
                    } catch (ignored: Exception) {
                        break
                    }
                }

                Gateway.instance!!.eventsManager.callEvent(PlayerQuitEvent(player!!))
                val quitStr = (if (properties.isLogPlayerIPAddresses) inetAddress.hostName else "<ip address withheld>") + ":" + clientSocket.port + "|" + player!!.name
                Gateway.instance!!.console.sendMessage("[/$quitStr] <-> Player had disconnected!")
            }
        } catch (ignored: Exception) {
        } finally {
            try {
                keepAliveTask?.cancel()
                channel?.close()
                clientSocket.close()
            } catch (ignored: Exception) {
            }
            clientState = ClientState.DISCONNECTED
            player?.let { Gateway.instance?.unsafe?.b(it) }
            Gateway.instance?.serverConnection?.clients?.remove(this)
            isRunning = false
        }
    }

    private fun processMoveEvent(event: PlayerMoveEvent, originalTo: Location) {
        if (event.isCancelled) {
            val returnTo = event.from
            val cancel = PacketPlayOutPositionAndLook(returnTo.x, returnTo.y, returnTo.z, returnTo.yaw, returnTo.pitch, 1)
            sendPacket(cancel)
        } else {
            val to = event.to
            Gateway.instance!!.unsafe.a(player!!, to)
            if (originalTo != to) {
                val pos = PacketPlayOutPositionAndLook(to.x, to.y, to.z, to.yaw, to.pitch, 1)
                sendPacket(pos)
            }
            val response = PacketPlayOutUpdateViewPosition(player!!.location.x.toInt() shr 4, player!!.location.z.toInt() shr 4)
            sendPacket(response)
        }
    }

    enum class ClientState {
        LEGACY, HANDSHAKE, STATUS, LOGIN, CONFIGURATION, PLAY, DISCONNECTED
    }
}



