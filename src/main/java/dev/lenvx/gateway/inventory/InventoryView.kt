package dev.lenvx.gateway.inventory

import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutSetSlot
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutWindowData
import dev.lenvx.gateway.player.Player
import dev.lenvx.gateway.player.PlayerInventory
import net.kyori.adventure.text.Component
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class InventoryView(
    @get:JvmName("getPlayer") val player: Player,
    @get:JvmName("getTitle") var title: Component,
    @get:JvmName("getTopInventory") var topInventory: Inventory?,
    @get:JvmName("getBottomInventory") val bottomInventory: Inventory
) {

    @get:JvmName("getCarriedItem")
    @set:JvmName("setCarriedItem")
    var carriedItem: ItemStack? = null
    private val properties: MutableMap<Property, Int> = ConcurrentHashMap()

    @Deprecated("")
    val unsafe: Unsafe = Unsafe(this)

    @get:JvmName("getType")
    val type: InventoryType
        get() = topInventory?.type ?: bottomInventory.type

    @get:JvmName("getProperties")
    val propertiesMap: Map<Property, Int>
        get() = Collections.unmodifiableMap(properties)

    
    fun getInventory(rawSlot: Int): Inventory? {
        if (rawSlot == OUTSIDE || rawSlot == -1) {
            return null
        }
        require(rawSlot >= 0) { "Negative, non outside slot $rawSlot" }
        require(rawSlot < countSlots()) { "Slot $rawSlot greater than inventory slot count" }

        return if (rawSlot < (topInventory?.size ?: 0)) {
            topInventory
        } else {
            bottomInventory
        }
    }

    
    fun convertSlot(rawSlot: Int): Int {
        val numInTop = topInventory?.size ?: 0
        if (rawSlot < numInTop) {
            return rawSlot
        }

        var slot = rawSlot - numInTop

        if (type == InventoryType.CRAFTING || type == InventoryType.CREATIVE) {
            if (slot < 4) {
                return 39 - slot
            } else if (slot > 39) {
                return slot
            } else {
                slot -= 4
            }
        }

        return if (slot >= 27) {
            slot - 27
        } else {
            slot + 9
        }
    }

    
    fun getSlotType(slot: Int): InventoryType.SlotType {
        var slotType = InventoryType.SlotType.CONTAINER
        val topSize = topInventory?.size ?: 0
        if (slot in 0 until topSize) {
            when (type) {
                InventoryType.BLAST_FURNACE, InventoryType.FURNACE, InventoryType.SMOKER -> {
                    slotType = when (slot) {
                        2 -> InventoryType.SlotType.RESULT
                        1 -> InventoryType.SlotType.FUEL
                        else -> InventoryType.SlotType.CRAFTING
                    }
                }
                InventoryType.BREWING -> {
                    slotType = if (slot == 3) InventoryType.SlotType.FUEL else InventoryType.SlotType.CRAFTING
                }
                InventoryType.ENCHANTING, InventoryType.BEACON -> {
                    slotType = InventoryType.SlotType.CRAFTING
                }
                InventoryType.WORKBENCH, InventoryType.CRAFTING -> {
                    slotType = if (slot == 0) InventoryType.SlotType.RESULT else InventoryType.SlotType.CRAFTING
                }
                InventoryType.ANVIL, InventoryType.SMITHING, InventoryType.CARTOGRAPHY, InventoryType.GRINDSTONE, InventoryType.MERCHANT -> {
                    slotType = if (slot == 2) InventoryType.SlotType.RESULT else InventoryType.SlotType.CRAFTING
                }
                InventoryType.STONECUTTER -> {
                    slotType = if (slot == 1) InventoryType.SlotType.RESULT else InventoryType.SlotType.CRAFTING
                }
                InventoryType.LOOM -> {
                    slotType = if (slot == 3) InventoryType.SlotType.RESULT else InventoryType.SlotType.CRAFTING
                }
                else -> {}
            }
        } else {
            if (slot < 0) {
                slotType = InventoryType.SlotType.OUTSIDE
            } else if (type == InventoryType.CRAFTING) {
                if (slot < 9) {
                    slotType = InventoryType.SlotType.ARMOR
                } else if (slot > 35) {
                    slotType = InventoryType.SlotType.QUICKBAR
                }
            } else if (slot >= (countSlots() - (9 + 4 + 1))) {
                slotType = InventoryType.SlotType.QUICKBAR
            }
        }
        return slotType
    }

    
    fun countSlots(): Int {
        return (topInventory?.size ?: 0) + bottomInventory.size
    }

    fun close() {
        player.closeInventory()
    }

    fun isSlot(index: Int): Boolean {
        var actualIndex = index
        if (topInventory != null) {
            if (actualIndex < topInventory!!.size) {
                return true
            }
            actualIndex -= topInventory!!.size
        }
        return if (bottomInventory is PlayerInventory) {
            actualIndex < 36
        } else {
            actualIndex < bottomInventory.size
        }
    }

    fun getItem(index: Int): ItemStack? {
        val inv = getInventory(index) ?: return null
        return inv.getItem(convertSlot(index))
    }

    fun setItem(index: Int, itemStack: ItemStack?) {
        getInventory(index)?.setItem(convertSlot(index), itemStack)
    }

    fun setProperty(prop: Property, value: Int) {
        if (topInventory != null && prop.type == topInventory!!.type) {
            val id = topInventory!!.getUnsafe().c()?.get(player)
            if (id != null) {
                properties[prop] = value
                val packet = PacketPlayOutWindowData(id, prop.id, value)
                try {
                    player.clientConnection.sendPacket(packet)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun updateView() {
        topInventory?.updateInventory(player)
        bottomInventory.updateInventory(player)
        try {
            player.clientConnection.sendPacket(PacketPlayOutSetSlot(-1, -1, 0, carriedItem))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Deprecated("")
    class Unsafe @Deprecated("") constructor(private val inventoryView: InventoryView) {
        @Deprecated("")
        fun a(topInventory: Inventory?, title: Component) {
            inventoryView.topInventory = topInventory
            inventoryView.title = title
            inventoryView.properties.clear()
        }

        @Deprecated("")
        fun a(): Int {
            return if (inventoryView.topInventory != null) {
                inventoryView.topInventory!!.getUnsafe().c()?.getOrDefault(inventoryView.player, -1) ?: -1
            } else {
                inventoryView.bottomInventory.getUnsafe().c()?.getOrDefault(inventoryView.player, -1) ?: -1
            }
        }
    }

    
    enum class Property(val id: Int, @get:JvmName("getType") val type: InventoryType) {
        BREW_TIME(0, InventoryType.BREWING),
        FUEL_TIME(1, InventoryType.BREWING),
        BURN_TIME(0, InventoryType.FURNACE),
        TICKS_FOR_CURRENT_FUEL(1, InventoryType.FURNACE),
        COOK_TIME(2, InventoryType.FURNACE),
        TICKS_FOR_CURRENT_SMELTING(3, InventoryType.FURNACE),
        ENCHANT_BUTTON1(0, InventoryType.ENCHANTING),
        ENCHANT_BUTTON2(1, InventoryType.ENCHANTING),
        ENCHANT_BUTTON3(2, InventoryType.ENCHANTING),
        ENCHANT_XP_SEED(3, InventoryType.ENCHANTING),
        ENCHANT_ID1(4, InventoryType.ENCHANTING),
        ENCHANT_ID2(5, InventoryType.ENCHANTING),
        ENCHANT_ID3(6, InventoryType.ENCHANTING),
        ENCHANT_LEVEL1(7, InventoryType.ENCHANTING),
        ENCHANT_LEVEL2(8, InventoryType.ENCHANTING),
        ENCHANT_LEVEL3(9, InventoryType.ENCHANTING),
        LEVELS(0, InventoryType.BEACON),
        PRIMARY_EFFECT(1, InventoryType.BEACON),
        SECONDARY_EFFECT(2, InventoryType.BEACON),
        REPAIR_COST(0, InventoryType.ANVIL),
        BOOK_PAGE(0, InventoryType.LECTERN);
    }

    companion object {
        @JvmField
        val OUTSIDE = -999
    }
}

