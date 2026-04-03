package dev.lenvx.gateway

import com.google.gson.GsonBuilder
import dev.lenvx.gateway.bossbar.KeyedBossBar
import dev.lenvx.gateway.commands.CommandSender
import dev.lenvx.gateway.commands.DefaultCommands
import dev.lenvx.gateway.consolegui.GUI
import dev.lenvx.gateway.events.EventsManager
import dev.lenvx.gateway.file.ServerProperties
import dev.lenvx.gateway.inventory.AnvilInventory
import dev.lenvx.gateway.inventory.CustomInventory
import dev.lenvx.gateway.inventory.Inventory
import dev.lenvx.gateway.inventory.InventoryHolder
import dev.lenvx.gateway.inventory.InventoryType
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.metrics.Metrics
import dev.lenvx.gateway.metrics.RuntimeMetrics
import dev.lenvx.gateway.network.ServerConnection
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutBoss
import dev.lenvx.gateway.permissions.PermissionsManager
import dev.lenvx.gateway.player.Player
import dev.lenvx.gateway.plugins.GatewayPlugin
import dev.lenvx.gateway.plugins.PluginManager
import dev.lenvx.gateway.scheduler.GatewayScheduler
import dev.lenvx.gateway.scheduler.Tick
import dev.lenvx.gateway.utils.CustomStringUtils
import dev.lenvx.gateway.utils.ImageUtils
import dev.lenvx.gateway.utils.NetworkUtils
import dev.lenvx.gateway.world.Environment
import dev.lenvx.gateway.world.Schematic
import dev.lenvx.gateway.world.World
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.querz.nbt.io.NBTUtil
import net.querz.nbt.tag.CompoundTag
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.ParseException
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import javax.swing.UnsupportedLookAndFeelException

class Gateway @Throws(IOException::class, ParseException::class, NumberFormatException::class, ClassNotFoundException::class, InterruptedException::class) constructor() {

    @JvmField
    val SERVER_IMPLEMENTATION_VERSION = "1.21.11"
    @JvmField
    val SERVER_IMPLEMENTATION_PROTOCOL = 774
    @JvmField
    val GATEWAY_IMPLEMENTATION_VERSION: String

    private val runningFlag: AtomicBoolean = AtomicBoolean(true)

    val serverConnection: ServerConnection
    val console: Console

    private val worldsList: MutableList<World> = CopyOnWriteArrayList()
    @get:JvmName("getWorldsList")
    val worlds: List<World>
        get() = ArrayList(worldsList)
    @get:JvmName("getPlayersSet")
    val players: Set<Player>
        get() = HashSet(playersByUUID.values)
    val playersByName: MutableMap<String, Player> = ConcurrentHashMap()
    val playersByUUID: MutableMap<UUID, Player> = ConcurrentHashMap()
    @get:JvmName("getBossBarsMap")
    val bossBars: MutableMap<Key, KeyedBossBar> = ConcurrentHashMap()
    val serverProperties: ServerProperties

    val pluginManager: PluginManager
    val eventsManager: EventsManager
    val permissionsManager: PermissionsManager
    val pluginFolder: File

    val heartBeat: Tick
    val scheduler: GatewayScheduler

    val metrics: Metrics
    val runtimeMetrics: RuntimeMetrics

    val entityIdCount: AtomicInteger = AtomicInteger()

    @get:JvmName("getUnsafeInternal")
    @Deprecated("")
    val unsafe: Unsafe

    init {
        instance = this
        unsafe = Unsafe(this)

        if (!noGui) {
            while (!GUI.loadFinish) {
                TimeUnit.MILLISECONDS.sleep(500)
            }
            console = Console(null, System.`out`, System.`err`)
        } else {
            console = Console(System.`in`, System.`out`, System.`err`)
        }

        GATEWAY_IMPLEMENTATION_VERSION = gatewayVersion
        console.sendMessage("Loading Gateway Version $GATEWAY_IMPLEMENTATION_VERSION on Minecraft $SERVER_IMPLEMENTATION_VERSION")

        val spName = "server.properties"
        val sp = File(spName)
        if (!sp.exists()) {
            javaClass.classLoader.getResourceAsStream(spName)?.use { `in` ->
                Files.copy(`in`, sp.toPath())
            }
        }
        serverProperties = ServerProperties(sp)

        if (!serverProperties.isBungeeCord) {
            console.sendMessage("If you are using BungeeCord, consider turning that on in the settings!")
        } else {
            console.sendMessage("Starting Gateway server in BungeeCord mode!")
        }

        val defaultWorld = loadDefaultWorld()
        if (defaultWorld == null) {
            console.sendMessage("Failed to load default world. Server will exit.")
            System.exit(1)
        } else {
            worldsList.add(defaultWorld)
        }
        val spawn = serverProperties.worldSpawn
        serverProperties.worldSpawn = Location(getWorld(serverProperties.levelName.value()) ?: defaultWorld!!, spawn.x, spawn.y, spawn.z, spawn.yaw, spawn.pitch)

        if (!NetworkUtils.available(serverProperties.serverPort)) {
            console.sendMessage("")
            console.sendMessage("*****FAILED TO BIND PORT [" + serverProperties.serverPort + "]*****")
            console.sendMessage("*****PORT ALREADY IN USE*****")
            console.sendMessage("*****PERHAPS ANOTHER INSTANCE OF THE SERVER IS ALREADY RUNNING?*****")
            console.sendMessage("")
            System.exit(2)
        }

        val permissionName = "permission.yml"
        val permissionFile = File(permissionName)
        if (!permissionFile.exists()) {
            javaClass.classLoader.getResourceAsStream(permissionName)?.use { `in` ->
                Files.copy(`in`, permissionFile.toPath())
            }
        }

        scheduler = GatewayScheduler()
        heartBeat = Tick(this)

        permissionsManager = PermissionsManager()
        permissionsManager.loadDefaultPermissionFile(permissionFile)

        eventsManager = EventsManager()

        pluginFolder = File("plugins")
        pluginFolder.mkdirs()

        pluginManager = PluginManager(DefaultCommands(), pluginFolder)
        try {
            pluginManager.loadPlugins()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        logCompatibilityHints()

        for (plugin in pluginManager.plugins.values) {
            try {
                console.sendMessage("Enabling plugin " + plugin.name + " " + plugin.info.version)
                plugin.onEnable()
            } catch (e: Throwable) {
                RuntimeException("Error while enabling " + plugin.name + " " + plugin.info.version, e).printStackTrace()
            }
        }

        serverConnection = ServerConnection(serverProperties.serverIp ?: "", serverProperties.serverPort, false)

        metrics = Metrics()
        runtimeMetrics = RuntimeMetrics()

        Runtime.getRuntime().addShutdownHook(Thread {
            instance?.terminate()
        })

        console.run()
    }

    @Deprecated("")
    fun getUnsafe(): Unsafe {
        return unsafe
    }

    @Throws(IOException::class)
    private fun loadDefaultWorld(): World? {
        console.sendMessage("Loading world " + serverProperties.levelName + " with the schematic file " + serverProperties.schemFileName + " ...")

        val schem = File(serverProperties.schemFileName)

        if (!schem.exists()) {
            console.sendMessage("Schematic file " + serverProperties.schemFileName + " for world " + serverProperties.levelName + " not found!")
            console.sendMessage("Creating default world...")
            Gateway::class.java.classLoader.getResourceAsStream("spawn.schem")?.use { `in` ->
                Files.copy(`in`, schem.toPath())
            }
        }

        return try {
            val world = Schematic.toWorld(serverProperties.levelName.value(), Environment.fromKey(serverProperties.levelDimension) ?: Environment.NORMAL, NBTUtil.read(schem).tag as CompoundTag)
            console.sendMessage("Loaded world " + serverProperties.levelName + "!")
            world
        } catch (e: Throwable) {
            console.sendMessage("Unable to load world " + serverProperties.schemFileName + "!")
            e.printStackTrace()
            console.sendMessage("Server will exit!")
            System.exit(1)
            null
        }
    }

    fun registerWorld(world: World) {
        if (!worlds.contains(world)) {
            worldsList.add(world)
        } else {
            throw RuntimeException("World already registered")
        }
    }

    fun unregisterWorld(world: World) {
        if (worlds.indexOf(world) == 0) {
            throw RuntimeException("World already registered")
        } else if (!worlds.contains(world)) {
            throw RuntimeException("World not registered")
        } else {
            for (player in world.getPlayers()) {
                player.teleport(serverProperties.worldSpawn)
            }
            worldsList.remove(world)
        }
    }

    fun createBossBar(key: Key, name: Component, progress: Float, color: BossBar.Color, overlay: BossBar.Overlay, vararg flags: BossBar.Flag): KeyedBossBar {
        val keyedBossBar = dev.lenvx.gateway.bossbar.Unsafe.a(key, BossBar.bossBar(name, progress, color, overlay, HashSet(listOf(*flags))))
        bossBars[key] = keyedBossBar
        return keyedBossBar
    }

    fun removeBossBar(key: Key) {
        val keyedBossBar = bossBars.remove(key) ?: return
        keyedBossBar.properties.removeListener(keyedBossBar.unsafe.a())
        keyedBossBar.unsafe.b()
        val packetPlayOutBoss = PacketPlayOutBoss(keyedBossBar, PacketPlayOutBoss.BossBarAction.REMOVE)
        for (player in keyedBossBar.players) {
            try {
                player.clientConnection.sendPacket(packetPlayOutBoss)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun getBossBars(): Map<Key, KeyedBossBar> {
        return Collections.unmodifiableMap(bossBars)
    }

    fun getPlayers(): Set<Player> {
        return HashSet(playersByUUID.values)
    }

    fun getPlayer(name: String): Player? {
        return playersByName[name]
    }

    fun getPlayer(uuid: UUID): Player? {
        return playersByUUID[uuid]
    }

    fun findPlayer(identifier: String): Player? {
        val byName = getPlayer(identifier)
        if (byName != null) {
            return byName
        }
        return runCatching { UUID.fromString(identifier) }.getOrNull()?.let { getPlayer(it) }
    }

    fun requirePlayer(uuid: UUID): Player {
        return getPlayer(uuid) ?: throw NoSuchElementException("Player not found: $uuid")
    }

    fun requirePlayer(name: String): Player {
        return getPlayer(name) ?: throw NoSuchElementException("Player not found: $name")
    }

    fun getWorlds(): List<World> {
        return ArrayList(worlds)
    }

    fun getWorld(name: String): World? {
        for (world in worlds) {
            if (world.name.equals(name, ignoreCase = true)) {
                return world
            }
        }
        return null
    }

    fun requireWorld(name: String): World {
        return getWorld(name) ?: throw NoSuchElementException("World not found: $name")
    }

    fun isProxyForwardingEnabled(): Boolean {
        return serverProperties.isBungeeCord || serverProperties.isVelocityModern || serverProperties.isBungeeGuard
    }

    fun getPlugins(): List<GatewayPlugin> {
        return pluginManager.plugins.values.toList()
    }

    private fun logCompatibilityHints() {
        if (serverProperties.forgeHandshakeEmulationEnabled) {
            console.sendMessage("Forge/NeoForge handshake emulation is enabled for fallback compatibility.")
        }
        val translationMode = serverProperties.protocolTranslationMode
        if (translationMode == ServerProperties.ProtocolTranslationMode.BUILTIN) {
            console.sendMessage("Built-in protocol translation is enabled. Mixed-version mode is active.")
            return
        }
        console.sendMessage("Protocol translation is disabled. Native client support is ${SERVER_IMPLEMENTATION_VERSION} only.")
    }

    @Throws(IOException::class)
    fun buildServerListResponseJson(version: String, protocol: Int, motd: Component, maxPlayers: Int, playersOnline: Int, favicon: BufferedImage?): String {
        val json = JSONObject()

        val versionJson = JSONObject()
        versionJson["name"] = version
        versionJson["protocol"] = protocol
        json["version"] = versionJson

        val playersJson = JSONObject()
        playersJson["max"] = maxPlayers
        playersJson["online"] = playersOnline
        json["players"] = playersJson

        json["description"] = "%MOTD%"

        if (favicon != null) {
            if (favicon.width == 64 && favicon.height == 64) {
                val base64 = "data:image/png;base64," + ImageUtils.imgToBase64String(favicon, "png")
                json["favicon"] = base64
            } else {
                console.sendMessage("Server List Favicon must be 64 x 64 in size!")
            }
        }

        val modInfoJson = JSONObject()
        modInfoJson["type"] = "FML"
        modInfoJson["modList"] = JSONArray()
        json["modinfo"] = modInfoJson

        val treeMap = TreeMap<String, Any?>(String.CASE_INSENSITIVE_ORDER)
        treeMap.putAll(json as Map<out String, Any?>)

        val g = GsonBuilder().create()

        return g.toJson(treeMap).replace("\"%MOTD%\"", GsonComponentSerializer.gson().serialize(motd))
    }

    fun buildLegacyPingResponse(version: String, motd: Component, maxPlayers: Int, playersOnline: Int): String {
        val begin = "Â§1"
        return listOf(begin, "127", version, listOf(motd).stream().map { each -> LegacyComponentSerializer.legacySection().serialize(each!!) }.collect(Collectors.toList()).joinToString(""), playersOnline.toString(), maxPlayers.toString()).joinToString("\u0000")
    }

    protected fun terminate() {
        runningFlag.set(false)
        console.sendMessage("Stopping Server...")

        for (plugin in pluginManager.plugins.values) {
            try {
                console.sendMessage("Disabling plugin " + plugin.name + " " + plugin.info.version)
                plugin.onDisable()
            } catch (e: Throwable) {
                RuntimeException("Error while enabling " + plugin.name + " " + plugin.info.version, e).printStackTrace()
            }
        }

        heartBeat.waitAndKillThreads(5000)

        for (player in getPlayers()) {
            player.disconnect("Server closed")
        }
        while (getPlayers().isNotEmpty()) {
            try {
                TimeUnit.MILLISECONDS.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        console.sendMessage("Server closed")
        console.logs.close()
    }

    fun stopServer() {
        System.exit(0)
    }

    fun isRunning(): Boolean {
        return runningFlag.get()
    }

    fun getNextEntityId(): Int {
        return entityIdCount.getAndUpdate { i -> if (i == Int.MAX_VALUE) 0 else i + 1 }
    }

    fun dispatchCommand(sender: CommandSender, str: String) {
        tryDispatchCommand(sender, str)
    }

    fun dispatchCommand(sender: CommandSender, vararg args: String) {
        tryDispatchCommand(sender, *args)
    }

    fun tryDispatchCommand(sender: CommandSender, str: String): Boolean {
        val normalized = if (str.startsWith("/")) str.substring(1) else str
        val command = CustomStringUtils.splitStringToArgs(normalized)
        return tryDispatchCommand(sender, *command)
    }

    fun tryDispatchCommand(sender: CommandSender, vararg args: String): Boolean {
        if (args.isEmpty()) {
            return false
        }
        try {
            instance?.pluginManager?.fireExecutors(sender, args as Array<String>)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private val gatewayVersion: String
        get() {
            try {
                val manifests = javaClass.classLoader.getResources("META-INF/MANIFEST.MF")
                while (manifests.hasMoreElements()) {
                    val url = manifests.nextElement()
                    BufferedReader(InputStreamReader(url.openStream())).use { br ->
                        val gatewayLine = br.lines().filter { each -> each.startsWith("Gateway-Version:") }.findFirst()
                        if (gatewayLine.isPresent) {
                            return gatewayLine.get().substring(16).trim { it <= ' ' }
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return "Unknown"
        }

    fun createInventory(title: Component?, slots: Int, holder: InventoryHolder?): Inventory {
        return CustomInventory.create(title, slots, holder)
    }

    fun createInventory(type: InventoryType, holder: InventoryHolder?): Inventory {
        return createInventory(null, type, holder)
    }

    fun createInventory(title: Component?, type: InventoryType, holder: InventoryHolder?): Inventory {
        if (!type.isCreatable) {
            throw UnsupportedOperationException("This InventoryType cannot be created.")
        }
        return when (type) {
            InventoryType.ANVIL -> AnvilInventory(title, holder)
            else -> throw UnsupportedOperationException("This InventoryType has not been implemented yet.")
        }
    }

    companion object {
        const val GATEWAY_BRAND = "Gateway"
        @get:JvmName("getInstanceNullable")
        var instance: Gateway? = null
            private set
        @JvmField
        var noGui = false

        @JvmStatic
        fun getInstance(): Gateway {
            return instance ?: throw IllegalStateException("Gateway has not been initialized")
        }

        @JvmStatic
        @Throws(IOException::class, ParseException::class, NumberFormatException::class, ClassNotFoundException::class, InterruptedException::class)
        fun main(args: Array<String>) {
            for (flag in args) {
                if (flag == "--nogui" || flag == "nogui") {
                    noGui = true
                } else if (flag == "--help") {
                    println("Accepted flags:")
                    println(" --nogui <- Disable the GUI")
                    System.exit(0)
                } else {
                    println("Unknown flag: \"$flag\". Ignoring...")
                }
            }
            if (GraphicsEnvironment.isHeadless()) {
                noGui = true
            }
            if (!noGui) {
                println("Launching Server GUI.. Add \"--nogui\" in launch arguments to disable")
                val t1 = Thread {
                    try {
                        GUI.main()
                    } catch (e: UnsupportedLookAndFeelException) {
                        e.printStackTrace()
                    } catch (e: ClassNotFoundException) {
                        e.printStackTrace()
                    } catch (e: InstantiationException) {
                        e.printStackTrace()
                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    }
                }
                t1.start()
            }

            Gateway()
        }
    }
}


