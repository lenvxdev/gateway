@file:Suppress("removal")

package dev.lenvx.gateway.player

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.commands.CommandSender
import dev.lenvx.gateway.entity.DataWatcher
import dev.lenvx.gateway.entity.DataWatcher.WatchableField
import dev.lenvx.gateway.entity.DataWatcher.WatchableObjectType
import dev.lenvx.gateway.entity.EntityEquipment
import dev.lenvx.gateway.entity.EntityType
import dev.lenvx.gateway.entity.LivingEntity
import dev.lenvx.gateway.events.inventory.InventoryCloseEvent
import dev.lenvx.gateway.events.inventory.InventoryOpenEvent
import dev.lenvx.gateway.events.player.PlayerChatEvent
import dev.lenvx.gateway.events.player.PlayerTeleportEvent
import dev.lenvx.gateway.inventory.*
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.network.ClientConnection
import dev.lenvx.gateway.network.protocol.packets.*
import dev.lenvx.gateway.sounds.SoundEffect
import dev.lenvx.gateway.utils.BungeeCordAdventureConversionUtils
import dev.lenvx.gateway.utils.GameMode
import dev.lenvx.gateway.utils.MessageSignature
import net.kyori.adventure.audience.MessageType
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.Emitter
import net.kyori.adventure.sound.SoundStop
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import java.io.IOException
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicInteger

open class Player(
	val clientConnection: ClientConnection,
	protected val username: String,
	uuid: UUID,
	entityId: Int,
	location: Location,
	val playerInteractManager: PlayerInteractManager
) : LivingEntity(EntityType.PLAYER, entityId, uuid, location.world, location.x, location.y, location.z, location.yaw, location.pitch), CommandSender, InventoryHolder {

	companion object {
		const val CHAT_DEFAULT_FORMAT = "<%name%> %message%"
	}

	var gamemode: GameMode = GameMode.SURVIVAL
	protected val watcher: DataWatcher = DataWatcher(this)
	var selectedSlot: Byte = 0
		set(value) {
			if (field != value) {
				try {
					clientConnection.sendPacket(PacketPlayOutHeldItemChange(value))
				} catch (e: IOException) {
					e.printStackTrace()
				}
				field = value
			}
		}

	val playerInventory: PlayerInventory = PlayerInventory(this)
	val inventoryView: InventoryView = InventoryView(this, Component.empty(), EmptyInventory.create(this), playerInventory)
	private val containerIdCounter = AtomicInteger(1)

	@WatchableField(MetadataIndex = 15, WatchableObjectType = WatchableObjectType.BYTE)
	var mainHand: Byte = 1
	
	@WatchableField(MetadataIndex = 16, WatchableObjectType = WatchableObjectType.BYTE)
	var skinLayers: Byte = 0
	
	@WatchableField(MetadataIndex = 17, WatchableObjectType = WatchableObjectType.FLOAT)
	var additionalHearts: Float = 0.0f
	
	@WatchableField(MetadataIndex = 18, WatchableObjectType = WatchableObjectType.VARINT)
	var score: Int = 0

	init {
		this.entityId = entityId
		this.playerInteractManager.setPlayer(this)
		this.watcher.update()
	}

	protected fun nextContainerId(): Int {
		return containerIdCounter.updateAndGet { i -> if (i + 1 > Byte.MAX_VALUE) 1 else i + 1 }
	}

	override fun getDataWatcher(): DataWatcher = watcher

	override fun isValid(): Boolean = Gateway.instance?.players?.contains(this) ?: false

	override fun remove() {}

	override val name: String get() = username

	override fun hasPermission(permission: String): Boolean {
		return Gateway.instance?.permissionsManager?.hasPermission(this, permission) ?: false
	}

	override fun teleport(location: Location) {
		val event = Gateway.instance?.eventsManager?.callEvent(PlayerTeleportEvent(this, this.location, location)) ?: PlayerTeleportEvent(this, this.location, location)
		if (!event.isCancelled) {
			val to = event.to
			super.teleport(to)
			try {
				if (world != to.world) {
					clientConnection.sendPacket(PacketPlayOutRespawn(to.world, 0, gamemode, false, false, true))
				}
				clientConnection.sendPacket(PacketPlayOutPositionAndLook(to.x, to.y, to.z, to.yaw, to.pitch, 1))
			} catch (ignored: IOException) {}
		}
	}

	internal fun setLocation(location: Location) {
		super.teleport(location)
	}

	@Throws(IOException::class)
	fun sendPluginMessage(channel: Key, data: ByteArray) {
		sendPluginMessage(channel.toString(), data)
	}

	@Deprecated("Use sendPluginMessage(Key, ByteArray)")
	@Throws(IOException::class)
	fun sendPluginMessage(channel: String, data: ByteArray) {
		clientConnection.sendPluginMessage(channel, data)
	}

	override fun sendMessage(message: String, uuid: UUID) {
		sendMessage(Identity.identity(uuid), LegacyComponentSerializer.legacySection().deserialize(message))
	}

	@Deprecated("Use Adventure API")
	override fun sendMessage(component: BaseComponent, uuid: UUID) {
		sendMessage(arrayOf(component), uuid)
	}

	@Deprecated("Use Adventure API")
	override fun sendMessage(component: Array<BaseComponent>, uuid: UUID) {
		sendMessage(Identity.identity(uuid), BungeeCordAdventureConversionUtils.toComponent(*component))
	}

	override fun sendMessage(message: String) {
		sendMessage(LegacyComponentSerializer.legacySection().deserialize(message))
	}

	@Deprecated("Use Adventure API")
	override fun sendMessage(component: BaseComponent) {
		sendMessage(arrayOf(component))
	}

	@Deprecated("Use Adventure API")
	override fun sendMessage(component: Array<BaseComponent>) {
		sendMessage(BungeeCordAdventureConversionUtils.toComponent(*component))
	}

	fun disconnect() {
		disconnect(Component.translatable("multiplayer.disconnect.kicked"))
	}

	fun disconnect(reason: String) {
		disconnect(LegacyComponentSerializer.legacySection().deserialize(reason))
	}

	fun disconnect(reason: Component) {
		clientConnection.disconnect(reason)
	}

	@Deprecated("Use Adventure API")
	fun disconnect(reason: BaseComponent) {
		disconnect(arrayOf(reason))
	}

	@Deprecated("Use Adventure API")
	fun disconnect(reason: Array<BaseComponent>) {
		disconnect(BungeeCordAdventureConversionUtils.toComponent(*reason))
	}

	fun chat(message: String) {
		chat(message, false)
	}

	fun chat(message: String, verbose: Boolean) {
		chat(message, verbose, null, Instant.now())
	}

	fun chat(message: String, verbose: Boolean, saltSignature: MessageSignature?, time: Instant) {
		val gateway = Gateway.instance ?: return
		if (gateway.serverProperties.isAllowChat) {
			val event = gateway.eventsManager.callEvent(PlayerChatEvent(this, CHAT_DEFAULT_FORMAT, message, false))
			if (!event.isCancelled) {
				if (hasPermission("gatewayserver.chat") || hasPermission("limboserver.chat")) {
					val chat = event.format.replace("%name%", username).replace("%message%", event.message)
					gateway.console.sendMessage(chat)
					if (event.format == CHAT_DEFAULT_FORMAT) {
						for (each in gateway.players) {
							each.sendMessage(Identity.identity(uuid), Component.translatable("chat.type.text").args(Component.text(this.name), Component.text(event.message)), MessageType.CHAT, saltSignature, time)
						}
					} else {
						for (each in gateway.players) {
							each.sendMessage(Identity.identity(uuid), Component.text(chat), MessageType.SYSTEM, saltSignature, time)
						}
					}
				} else if (verbose) {
					sendMessage(ChatColor.RED.toString() + "You do not have permission to chat!")
				}
			}
		}
	}

	fun setResourcePack(url: String, hash: String, forced: Boolean) {
		setResourcePack(url, hash, forced, null as Component?)
	}

	@Deprecated("Use Adventure API")
	fun setResourcePack(url: String, hash: String, forced: Boolean, promptMessage: BaseComponent?) {
		setResourcePack(url, hash, forced, promptMessage?.let { arrayOf(it) })
	}

	@Deprecated("Use Adventure API")
	fun setResourcePack(url: String, hash: String, forced: Boolean, promptMessage: Array<BaseComponent>?) {
		setResourcePack(url, hash, forced, promptMessage?.let { BungeeCordAdventureConversionUtils.toComponent(*it) })
	}

	fun setResourcePack(url: String, hash: String, forced: Boolean, promptMessage: Component?) {
		try {
			clientConnection.sendPacket(ClientboundResourcePackPushPacket(UUID.randomUUID(), url, hash, forced, promptMessage))
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	@Deprecated("Use Adventure API")
	fun setPlayerListHeaderFooter(header: Array<BaseComponent>?, footer: Array<BaseComponent>?) {
		sendPlayerListHeaderAndFooter(
			header?.let { BungeeCordAdventureConversionUtils.toComponent(*it) } ?: Component.empty(),
			footer?.let { BungeeCordAdventureConversionUtils.toComponent(*it) } ?: Component.empty()
		)
	}

	@Deprecated("Use Adventure API")
	fun setPlayerListHeaderFooter(header: BaseComponent?, footer: BaseComponent?) {
		sendPlayerListHeaderAndFooter(
			header?.let { BungeeCordAdventureConversionUtils.toComponent(it) } ?: Component.empty(),
			footer?.let { BungeeCordAdventureConversionUtils.toComponent(it) } ?: Component.empty()
		)
	}

	fun setPlayerListHeaderFooter(header: String?, footer: String?) {
		sendPlayerListHeaderAndFooter(
			header?.let { LegacyComponentSerializer.legacySection().deserialize(it) } ?: Component.empty(),
			footer?.let { LegacyComponentSerializer.legacySection().deserialize(it) } ?: Component.empty()
		)
	}

	@Deprecated("Use Adventure API")
	fun setTitle(title: Array<BaseComponent>) {
		sendTitlePart(TitlePart.TITLE, BungeeCordAdventureConversionUtils.toComponent(*title))
	}

	@Deprecated("Use Adventure API")
	fun setTitle(title: BaseComponent) {
		sendTitlePart(TitlePart.TITLE, BungeeCordAdventureConversionUtils.toComponent(title))
	}

	fun setTitle(title: String) {
		sendTitlePart(TitlePart.TITLE, LegacyComponentSerializer.legacySection().deserialize(title))
	}

	@Deprecated("Use Adventure API")
	fun setSubTitle(subTitle: Array<BaseComponent>) {
		sendTitlePart(TitlePart.SUBTITLE, BungeeCordAdventureConversionUtils.toComponent(*subTitle))
	}

	@Deprecated("Use Adventure API")
	fun setSubTitle(subTitle: BaseComponent) {
		sendTitlePart(TitlePart.SUBTITLE, BungeeCordAdventureConversionUtils.toComponent(subTitle))
	}

	fun setSubTitle(subTitle: String) {
		sendTitlePart(TitlePart.SUBTITLE, LegacyComponentSerializer.legacySection().deserialize(subTitle))
	}

	fun setTitleTimer(fadeIn: Int, stay: Int, fadeOut: Int) {
		sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L)))
	}

	@Deprecated("Use Adventure API")
	fun setTitleSubTitle(title: Array<BaseComponent>, subTitle: Array<BaseComponent>, fadeIn: Int, stay: Int, fadeOut: Int) {
		setTitleTimer(fadeIn, stay, fadeOut)
		setTitle(title)
		setSubTitle(subTitle)
	}

	@Deprecated("Use Adventure API")
	fun setTitleSubTitle(title: BaseComponent, subTitle: BaseComponent, fadeIn: Int, stay: Int, fadeOut: Int) {
		setTitleSubTitle(arrayOf(title), arrayOf(subTitle), fadeIn, stay, fadeOut)
	}

	fun setTitleSubTitle(title: String, subTitle: String, fadeIn: Int, stay: Int, fadeOut: Int) {
		sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L)))
		sendTitlePart(TitlePart.SUBTITLE, LegacyComponentSerializer.legacySection().deserialize(subTitle))
		sendTitlePart(TitlePart.TITLE, LegacyComponentSerializer.legacySection().deserialize(title))
	}

	override fun sendMessage(source: Identity, message: Component, type: MessageType) {
		sendMessage(source, message, type, null, Instant.now())
	}

	fun sendMessage(source: Identity, message: Component, type: MessageType, signature: MessageSignature?, time: Instant) {
		try {
			val chat = when (type) {
				MessageType.CHAT -> ClientboundSystemChatPacket(message, false)
				MessageType.SYSTEM -> ClientboundSystemChatPacket(message, false)
				else -> ClientboundSystemChatPacket(message, false)
			}
			clientConnection.sendPacket(chat)
		} catch (ignored: IOException) {}
	}

	override fun openBook(book: Book) {
		throw UnsupportedOperationException("This function has not been implemented yet.")
	}

	override fun stopSound(stop: SoundStop) {
		val stopSound = PacketPlayOutStopSound(stop.sound(), stop.source())
		try {
			clientConnection.sendPacket(stopSound)
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun playSound(sound: Sound, emitter: Emitter) {
		throw UnsupportedOperationException("This function has not been implemented yet.")
	}

	override fun playSound(sound: Sound, x: Double, y: Double, z: Double) {
		val namedSoundEffect = PacketPlayOutNamedSoundEffect(
			SoundEffect.createVariableRangeEvent(sound.name()),
			sound.source(), x, y, z, sound.volume(), sound.pitch(), sound.seed().orElse(ThreadLocalRandom.current().nextLong())
		)
		try {
			clientConnection.sendPacket(namedSoundEffect)
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun playSound(sound: Sound) {
		playSound(sound, x, y, z)
	}

	override fun sendActionBar(message: Component) {
		try {
			clientConnection.sendPacket(ClientboundSetActionBarTextPacket(message))
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
		try {
			clientConnection.sendPacket(PacketPlayOutPlayerListHeaderFooter(header, footer))
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T) {
		try {
			when (part) {
				TitlePart.TITLE -> clientConnection.sendPacket(ClientboundSetTitleTextPacket(value as Component))
				TitlePart.SUBTITLE -> clientConnection.sendPacket(ClientboundSetSubtitleTextPacket(value as Component))
				TitlePart.TIMES -> {
					val times = value as Title.Times
					clientConnection.sendPacket(ClientboundSetTitlesAnimationPacket(
						(times.fadeIn().toMillis() / 50).toInt(),
						(times.stay().toMillis() / 50).toInt(),
						(times.fadeOut().toMillis() / 50).toInt()
					))
				}
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun clearTitle() {
		try {
			clientConnection.sendPacket(ClientboundClearTitlesPacket(false))
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	override fun resetTitle() {
		try {
			clientConnection.sendPacket(ClientboundClearTitlesPacket(true))
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	@Deprecated("Use KeyedBossBar.showPlayer instead")
	override fun showBossBar(bar: BossBar) {
		Gateway.instance?.bossBars?.values?.find { it.properties == bar }?.showPlayer(this)
	}

	@Deprecated("Use KeyedBossBar.hidePlayer instead")
	override fun hideBossBar(bar: BossBar) {
		Gateway.instance?.bossBars?.values?.find { it.properties == bar }?.hidePlayer(this)
	}

	override val inventory: PlayerInventory get() = playerInventory

	override val holder: InventoryHolder? get() = this

	fun updateInventory() {
		playerInventory.updateInventory(this)
	}

	val equipment: EntityEquipment
		get() = playerInventory

	fun openInventory(inventory: Inventory) {
		val title = if (inventory is TitledInventory) inventory.title else Component.translatable("container.chest")
		inventoryView.unsafe.a(inventory, title)
		val id = nextContainerId()
		(inventory.getUnsafe().c() as? MutableMap<Player, Int>)?.set(this, id)
		val event = Gateway.instance?.eventsManager?.callEvent(InventoryOpenEvent(inventoryView)) ?: InventoryOpenEvent(inventoryView)
		if (event.isCancelled) {
			inventoryView.unsafe.a(null, Component.empty())
			(inventory.getUnsafe().c() as? MutableMap<Player, Int>)?.remove(this)
		} else {
			val rawType = inventory.type.getRawType(inventory.size)
				?: throw IllegalStateException("Inventory type ${inventory.type} cannot be opened as a window")
			val packet = PacketPlayOutOpenWindow(id, rawType, title)
			try {
				clientConnection.sendPacket(packet)
			} catch (e: IOException) {
				e.printStackTrace()
			}
			inventoryView.updateView()
		}
	}

	fun closeInventory() {
		val inventory = inventoryView.topInventory
		if (inventory != null) {
			val id = inventory.getUnsafe().c()?.get(this)
			if (id != null) {
				Gateway.instance?.eventsManager?.callEvent(InventoryCloseEvent(inventoryView))
				inventoryView.unsafe.a(null, Component.empty())
				(inventory.getUnsafe().c() as? MutableMap<Player, Int>)?.remove(this)
				try {
					clientConnection.sendPacket(PacketPlayOutCloseWindow(id.toInt()))
				} catch (e: IOException) {
					e.printStackTrace()
				}
			}
		}
	}
}







