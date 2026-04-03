package dev.lenvx.gateway

import dev.lenvx.gateway.entity.DataWatcher
import dev.lenvx.gateway.entity.Entity
import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.player.Player
import dev.lenvx.gateway.utils.GameMode
import dev.lenvx.gateway.world.World

@Suppress("DeprecatedIsStillUsed")
@Deprecated("")
class Unsafe internal constructor(private val instance: Gateway) {

    private var playerUnsafe: dev.lenvx.gateway.player.Unsafe? = null
    private var worldUnsafe: dev.lenvx.gateway.world.Unsafe? = null

    init {
        try {
            val playerConstructor = dev.lenvx.gateway.player.Unsafe::class.java.getDeclaredConstructor()
            playerConstructor.isAccessible = true
            playerUnsafe = playerConstructor.newInstance()
            playerConstructor.isAccessible = false

            val worldConstructor = dev.lenvx.gateway.world.Unsafe::class.java.getDeclaredConstructor()
            worldConstructor.isAccessible = true
            worldUnsafe = worldConstructor.newInstance()
            worldConstructor.isAccessible = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Deprecated("")
    fun a(player: Player, mode: GameMode) {
        playerUnsafe?.a(player, mode)
    }

    @Deprecated("")
    fun a(player: Player, slot: Byte) {
        playerUnsafe?.a(player, slot)
    }

    @Deprecated("")
    fun a(player: Player, entityId: Int) {
        playerUnsafe?.a(player, entityId)
    }

    @Deprecated("")
    fun a(world: World, entity: Entity) {
        worldUnsafe?.a(world, entity)
    }

    @Deprecated("")
    fun b(world: World, entity: Entity): DataWatcher? {
        return worldUnsafe?.b(world, entity)
    }

    @Deprecated("")
    fun a(player: Player, location: Location) {
        playerUnsafe?.a(player, location)
    }

    @Deprecated("")
    fun a(player: Player) {
        instance.playersByName[player.name] = player
        instance.playersByUUID[player.uniqueId] = player
        instance.metrics.updatePlayersCount()
    }

    @Deprecated("")
    fun b(player: Player) {
        instance.getBossBars().values.forEach { it.hidePlayer(player) }
        instance.playersByName.remove(player.name)
        instance.playersByUUID.remove(player.uniqueId)
    }
}


