package dev.lenvx.gateway.utils

import dev.lenvx.gateway.inventory.InventoryView
import dev.lenvx.gateway.inventory.ItemStack
import dev.lenvx.gateway.network.protocol.packets.PacketPlayInWindowClick
import dev.lenvx.gateway.player.Player
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.floor

object InventoryClickUtils {

    private val QUICK_CRAFT_INFO: MutableMap<Player, QuickCraftInfo> = Collections.synchronizedMap(WeakHashMap())

    @JvmStatic
    @Synchronized
    fun handle(player: Player, packetplayinwindowclick: PacketPlayInWindowClick) {
        if (packetplayinwindowclick.clickType == dev.lenvx.gateway.inventory.InventoryClickType.QUICK_CRAFT) {
            val quickCraft = synchronized(QUICK_CRAFT_INFO) {
                QUICK_CRAFT_INFO.computeIfAbsent(player) { QuickCraftInfo() }
            }
            if (packetplayinwindowclick.slotNum < 0 || player.inventoryView.carriedItem == null) {
                quickCraft.resetQuickCraft()
            }
        }
    }

    @JvmStatic
    fun getQuickcraftType(i: Int): Int {
        return i shr 2 and 3
    }

    @JvmStatic
    fun getQuickcraftHeader(i: Int): Int {
        return i and 3
    }

    @JvmStatic
    fun getQuickcraftMask(i: Int, j: Int): Int {
        return i and 3 or ((j and 3) shl 2)
    }

    @JvmStatic
    fun isValidQuickcraftType(i: Int, player: Player): Boolean {
        return i == 0 || (i == 1 || i == 2 && player.gamemode == GameMode.CREATIVE)
    }

    @JvmStatic
    fun canItemQuickReplace(view: InventoryView, slot: Int, itemstack: ItemStack, flag: Boolean): Boolean {
        val emptySlot = !view.isSlot(slot) || view.getItem(slot) == null
        val slotItem = view.getItem(slot)
        return if (!emptySlot && slotItem!!.isSimilar(itemstack)) {
            slotItem.amount() + (if (flag) 0 else itemstack.amount()) <= itemstack.getMaxStackSize()
        } else {
            emptySlot
        }
    }

    @JvmStatic
    fun getQuickCraftSlotCount(set: Set<Int>, i: Int, itemstack: ItemStack, j: Int): ItemStack {
        var stack = itemstack
        when (i) {
            0 -> stack = stack.amount(floor(stack.amount().toFloat() / set.size.toFloat()).toInt())
            1 -> stack = stack.amount(1)
            2 -> stack = stack.amount(stack.getMaxStackSize())
        }
        return stack.amount(stack.amount() + j)
    }

    class QuickCraftInfo {
        @JvmField
        var quickcraftType: Int = 0

        @JvmField
        var quickcraftStatus: Int = 0

        @JvmField
        val quickcraftSlots: MutableSet<Int> = ConcurrentHashMap.newKeySet()

        fun resetQuickCraft() {
            quickcraftStatus = 0
            quickcraftSlots.clear()
        }
    }
}

