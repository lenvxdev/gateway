package dev.lenvx.gateway.inventory

import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.player.Player
import net.kyori.adventure.key.Key
import java.util.function.IntUnaryOperator

interface Inventory : Iterable<ItemStack?> {

    val size: Int

    fun getMaxStackSize(): Int

    fun setMaxStackSize(size: Int)

    fun getItem(index: Int): ItemStack?

    fun setItem(index: Int, item: ItemStack?)

    fun addItem(vararg items: ItemStack): HashMap<Int, ItemStack>

    fun removeItem(vararg items: ItemStack): HashMap<Int, ItemStack>

    fun getContents(): Array<ItemStack?>

    fun setContents(items: Array<ItemStack?>)

    fun getStorageContents(): Array<ItemStack?>

    fun setStorageContents(items: Array<ItemStack?>)

    fun contains(material: Key): Boolean

    fun contains(item: ItemStack?): Boolean

    fun contains(material: Key, amount: Int): Boolean

    fun contains(item: ItemStack?, amount: Int): Boolean

    fun containsAtLeast(item: ItemStack?, amount: Int): Boolean

    fun all(material: Key): HashMap<Int, out ItemStack>

    fun all(item: ItemStack?): HashMap<Int, out ItemStack>

    fun first(material: Key): Int

    fun first(item: ItemStack?): Int

    fun firstEmpty(): Int

    fun isEmpty(): Boolean

    fun remove(material: Key)

    fun remove(item: ItemStack?)

    fun clear(index: Int)

    fun clear()

    fun getViewers(): Set<Player>

    val type: InventoryType

    val holder: InventoryHolder?

    override fun iterator(): MutableIterator<ItemStack?>

    fun iterator(index: Int): MutableIterator<ItemStack?>

    fun getLocation(): Location?

    fun updateInventory()

    fun updateInventory(player: Player)

    @Deprecated("")
    fun getUnsafe(): Unsafe

    @Deprecated("")
    interface Unsafe {
        @Deprecated("")
        fun a(index: Int, itemStack: ItemStack?)
        @Deprecated("")
        fun b(index: Int, itemStack: ItemStack?)
        @Deprecated("")
        fun a(): IntUnaryOperator?
        @Deprecated("")
        fun b(): IntUnaryOperator?
        @Deprecated("")
        fun c(): Map<Player, Int>?
    }
}

