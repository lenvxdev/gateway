package dev.lenvx.gateway.file

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.utils.GameMode
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Optional
import java.util.Properties
import java.util.UUID
import javax.imageio.ImageIO
import kotlin.system.exitProcess

class ServerProperties(val file: File) {

    companion object {
        const val COMMENT = "For explanation of what each option does, visit:\nhttps://lenvx.dev"
    }

    enum class ProtocolTranslationMode {
        BUILTIN,
        OFF;

        companion object {
            fun fromProperty(raw: String?): ProtocolTranslationMode {
                return when (raw?.trim()?.lowercase()) {
                    "builtin", "built-in", "internal", "" -> BUILTIN
                    "off", "disabled", "none", "native" -> OFF
                    "plugin", "external", "vialimbo", "viaproxy" -> BUILTIN
                    else -> BUILTIN
                }
            }
        }
    }

    var maxPlayers: Int = 0
        private set
    var serverPort: Int = 0
        private set
    var serverIp: String? = null
        private set
    lateinit var levelName: Key
        private set
    lateinit var schemFileName: String
        private set
    lateinit var levelDimension: Key
        private set
    lateinit var defaultGamemode: GameMode
        private set
    lateinit var worldSpawn: Location
    var isReducedDebugInfo: Boolean = false
        private set
    var isLogPlayerIPAddresses: Boolean = false
        private set
    var isAllowFlight: Boolean = false
        private set
    var isAllowChat: Boolean = false
        private set
    var maxPacketSizeBytes: Int = 0
        private set
    var maxInboundPacketsPerSecond: Int = 0
        private set
    var maxChatMessageLength: Int = 0
        private set
    var maxCommandLength: Int = 0
        private set
    var maxPluginMessagePayloadBytes: Int = 0
        private set
    var maxCommandArgs: Int = 0
        private set
    lateinit var motd: Component
        private set
    lateinit var versionString: String
        private set
    var protocol: Int = 0
        private set
    lateinit var protocolTranslationMode: ProtocolTranslationMode
        private set
    var isBungeeCord: Boolean = false
        private set
    var isVelocityModern: Boolean = false
        private set
    var isBungeeGuard: Boolean = false
        private set
    var forgeHandshakeEmulationEnabled: Boolean = false
        private set
    var forwardingSecrets: List<String>? = null
        private set
    var viewDistance: Int = 0
        private set
    var definedTicksPerSecond: Double = 0.0
        private set
    @get:JvmName("handshakeVerboseEnabled")
    var handshakeVerboseEnabled: Boolean = false
        private set
    @get:JvmName("enforceWhitelist")
    var enforceWhitelist: Boolean = false
        private set
    private lateinit var whitelist: MutableMap<UUID, String?>

    var resourcePackSHA1: String? = null
        private set
    var resourcePackLink: String? = null
        private set
    var resourcePackRequired: Boolean = false
        private set
    var resourcePackPrompt: Component? = null
        private set

    lateinit var tabHeader: Component
        private set
    lateinit var tabFooter: Component
        private set

    var favicon: Optional<BufferedImage> = Optional.empty()
        private set
    private val miniMessage = MiniMessage.miniMessage()

    init {
        val def = Properties()
        javaClass.classLoader.getResourceAsStream("server.properties")?.use { inputStream ->
            InputStreamReader(inputStream, StandardCharsets.UTF_8).use { defStream ->
                def.load(defStream)
            }
        }

        val prop = Properties()
        Files.newInputStream(file.toPath()).use { inputStream ->
            InputStreamReader(inputStream, StandardCharsets.UTF_8).use { stream ->
                prop.load(stream)
            }
        }

        for (entry in def.entries) {
            val key = entry.key.toString()
            val value = entry.value.toString()
            prop.putIfAbsent(key, value)
        }
        Files.newOutputStream(file.toPath()).use { outputStream ->
            PrintWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8)).use { pw ->
                prop.store(pw, COMMENT)
            }
        }

        protocol = Gateway.instance!!.SERVER_IMPLEMENTATION_PROTOCOL

        maxPlayers = prop.getProperty("max-players").toInt()
        serverPort = prop.getProperty("server-port").toInt()
        serverIp = prop.getProperty("server-ip")
        val level = prop.getProperty("level-name").split(";").toTypedArray()
        levelName = Key.key(level[0])
        schemFileName = level[1]
        levelDimension = Key.key(prop.getProperty("level-dimension"))
        defaultGamemode = GameMode.fromName(Key.key(prop.getProperty("default-gamemode")).value()) ?: GameMode.SURVIVAL
        val locStr = prop.getProperty("world-spawn").split(";").toTypedArray()
        val world = Gateway.instance!!.getWorld(locStr[0]) ?: run {
            Gateway.instance!!.console.sendMessage("World ${locStr[0]} not found! Using default world.")
            Gateway.instance!!.worlds[0]
        }
        val x = locStr[1].toDouble()
        val y = locStr[2].toDouble()
        val z = locStr[3].toDouble()
        val yaw = locStr[4].toFloat()
        val pitch = locStr[5].toFloat()
        worldSpawn = Location(world, x, y, z, yaw, pitch)
        isReducedDebugInfo = prop.getProperty("reduced-debug-info").toBoolean()
        isLogPlayerIPAddresses = prop.getProperty("log-player-ip-addresses").toBoolean()
        isAllowFlight = prop.getProperty("allow-flight").toBoolean()
        isAllowChat = prop.getProperty("allow-chat").toBoolean()
        maxPacketSizeBytes = prop.getProperty("max-packet-size-bytes").toInt().coerceIn(1024, 8 * 1024 * 1024)
        maxInboundPacketsPerSecond = prop.getProperty("max-inbound-packets-per-second").toInt().coerceIn(1, 2000)
        maxChatMessageLength = prop.getProperty("max-chat-message-length").toInt().coerceIn(1, 4096)
        maxCommandLength = prop.getProperty("max-command-length").toInt().coerceIn(1, 4096)
        maxPluginMessagePayloadBytes = prop.getProperty("max-plugin-message-payload-bytes").toInt().coerceIn(1, 1024 * 1024)
        maxCommandArgs = prop.getProperty("max-command-args").toInt().coerceIn(1, 128)
        motd = parseComponentProperty(prop.getProperty("motd"))
        versionString = prop.getProperty("version")
        val rawProtocolTranslationMode = prop.getProperty("protocol-translation-mode")
        protocolTranslationMode = ProtocolTranslationMode.fromProperty(rawProtocolTranslationMode)
        if (rawProtocolTranslationMode != null) {
            val normalizedMode = rawProtocolTranslationMode.trim().lowercase()
            if (normalizedMode == "plugin" || normalizedMode == "external" || normalizedMode == "vialimbo" || normalizedMode == "viaproxy") {
                Gateway.instance!!.console.sendMessage("External protocol translators are deprecated. Falling back to built-in protocol translation.")
            }
        }
        isBungeeCord = prop.getProperty("bungeecord").toBoolean()
        isVelocityModern = prop.getProperty("velocity-modern").toBoolean()
        isBungeeGuard = prop.getProperty("bungee-guard").toBoolean()
        forgeHandshakeEmulationEnabled = prop.getProperty("forge-handshake-emulation").toBoolean()
        if (isVelocityModern || isBungeeGuard) {
            val forwardingSecretsStr = prop.getProperty("forwarding-secrets")
            if (forwardingSecretsStr == null || forwardingSecretsStr == "") {
                Gateway.instance!!.console.sendMessage("Velocity Modern Forwarding or BungeeGuard is enabled but no forwarding-secret was found!")
                Gateway.instance!!.console.sendMessage("Server will exit!")
                exitProcess(1)
            }
            forwardingSecrets = listOf(*forwardingSecretsStr.split(";").toTypedArray())
            if (isBungeeCord) {
                Gateway.instance!!.console.sendMessage("BungeeCord is enabled but so is Velocity Modern Forwarding or BungeeGuard, We will automatically disable BungeeCord forwarding because of this")
                isBungeeCord = false
            }
            if (isVelocityModern && isBungeeGuard) {
                Gateway.instance!!.console.sendMessage("Both Velocity Modern Forwarding and BungeeGuard are enabled! Because of this we always prefer Modern Forwarding, disabling BungeeGuard")
                isBungeeGuard = false
            }
        }

        viewDistance = prop.getProperty("view-distance").toInt()
        definedTicksPerSecond = prop.getProperty("ticks-per-second").toDouble()
        handshakeVerboseEnabled = prop.getProperty("handshake-verbose").toBoolean()

        resourcePackLink = prop.getProperty("resource-pack")
        resourcePackSHA1 = prop.getProperty("resource-pack-sha1")
        resourcePackRequired = prop.getProperty("required-resource-pack").toBoolean()
        resourcePackPrompt = parseOptionalComponentProperty(prop.getProperty("resource-pack-prompt"))

        tabHeader = parseComponentProperty(prop.getProperty("tab-header"))
        tabFooter = parseComponentProperty(prop.getProperty("tab-footer"))

        val png = File("server-icon.png")
        if (png.exists()) {
            try {
                val image = ImageIO.read(png)
                if (image.height == 64 && image.width == 64) {
                    favicon = Optional.of(image)
                } else {
                    Gateway.instance!!.console.sendMessage("Unable to load server-icon.png! The image is not 64 x 64 in size!")
                }
            } catch (e: Exception) {
                Gateway.instance!!.console.sendMessage("Unable to load server-icon.png! Is it a png image?")
            }
        } else {
            Gateway.instance!!.console.sendMessage("No server-icon.png found")
            favicon = Optional.empty()
        }

        enforceWhitelist = prop.getProperty("enforce-whitelist").toBoolean()
        reloadWhitelist()

        Gateway.instance!!.console.sendMessage("Loaded server.properties")
    }

    fun reloadWhitelist() {
        val console = Gateway.instance!!.console
        val whitelistFile = File("whitelist.json")
        if (!whitelistFile.exists()) {
            try {
                Files.newOutputStream(whitelistFile.toPath()).use { outputStream ->
                    PrintWriter(OutputStreamWriter(outputStream)).use { pw ->
                        pw.println("[]")
                        pw.flush()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        whitelist = HashMap()
        try {
            val parser = JSONParser()
            Files.newInputStream(whitelistFile.toPath()).use { inputStream ->
                InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                    val obj = parser.parse(reader)

                    if (obj !is JSONArray) {
                        console.sendMessage("whitelist: expected [] got {}")
                        return
                    }

                    val array = obj
                    for (o in array) {
                        if (o !is JSONObject) {
                            console.sendMessage("whitelist: array element is not an object")
                            continue
                        }

                        val element = o
                        val uuidObj = element["uuid"]
                        if (uuidObj == null) {
                            console.sendMessage("whitelist: missing uuid attribute")
                            continue
                        }
                        if (uuidObj !is String) {
                            console.sendMessage("whitelist: uuid is not a string")
                            continue
                        }

                        val uuidStr = uuidObj
                        val uuid = UUID.fromString(uuidStr)
                        val name = if (element.containsKey("name")) element["name"] as String? else null
                        whitelist[uuid] = name
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val serverImplementationVersion: String
        get() = Gateway.instance!!.SERVER_IMPLEMENTATION_VERSION

    val serverModName: String
        get() = Gateway.GATEWAY_BRAND

    fun uuidWhitelisted(uuid: UUID): Boolean {
        return whitelist.containsKey(uuid)
    }

    private fun parseComponentProperty(raw: String?): Component {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            return Component.empty()
        }
        return parseComponentValue(value)
    }

    private fun parseOptionalComponentProperty(raw: String?): Component? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) {
            return null
        }
        return parseComponentValue(value)
    }

    private fun parseComponentValue(value: String): Component {
        return if (value.startsWith("{") || value.startsWith("[")) {
            try {
                GsonComponentSerializer.gson().deserialize(value)
            } catch (_: Exception) {
                miniMessage.deserialize(value)
            }
        } else {
            miniMessage.deserialize(value)
        }
    }
}



