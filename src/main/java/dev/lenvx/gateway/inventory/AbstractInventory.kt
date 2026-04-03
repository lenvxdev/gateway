package dev.lenvx.gateway.inventory

import dev.lenvx.gateway.location.Location
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutSetSlot
import dev.lenvx.gateway.network.protocol.packets.PacketPlayOutWindowItems
import dev.lenvx.gateway.player.Player
import net.kyori.adventure.key.Key
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReferenceArray
import java.util.function.IntUnaryOperator
import java.util.stream.IntStream
import java.util.stream.StreamSupport

abstract class AbstractInventory @JvmOverloads constructor(
    size: Int,
    override open val holder: InventoryHolder?,
    override val type: InventoryType,
    protected val slotConvertor: IntUnaryOperator? = null,
    protected val inverseSlotConvertor: IntUnaryOperator? = null
) : Inventory {

    @JvmField
    internal val viewers: MutableMap<Player, Int> = ConcurrentHashMap()
    protected val inventory: AtomicReferenceArray<ItemStack?> = AtomicReferenceArray(size)
    protected val listener: InventoryUpdateListener
    protected val actualSlotConvertor: IntUnaryOperator = slotConvertor ?: IntUnaryOperator.identity()
    protected val actualInverseSlotConvertor: IntUnaryOperator = inverseSlotConvertor ?: IntUnaryOperator.identity()

    private val unsafeImpl: Unsafe = Unsafe(this)

    private var maxStackSize: Int = 64

    init {
        this.listener = InventoryUpdateListener { _, slot, _, newItem ->
            for ((key, value) in viewers) {
                try {
                    val packet = PacketPlayOutSetSlot(value, 0, actualSlotConvertor.applyAsInt(slot), newItem)
                    key.clientConnection.sendPacket(packet)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun updateInventory(player: Player) {
        val windowId = viewers[player] ?: return
        val itemStackArray = arrayOfNulls<ItemStack>(IntStream.range(0, inventory.length()).map(actualSlotConvertor).max().orElse(-1) + 1)
        for (i in 0 until inventory.length()) {
            itemStackArray[actualSlotConvertor.applyAsInt(i)] = getItem(i)
        }
        try {
            val packet = PacketPlayOutWindowItems(windowId, 0, listOf(*itemStackArray), ItemStack.AIR)
            player.clientConnection.sendPacket(packet)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun updateInventory() {
        val itemStackArray = arrayOfNulls<ItemStack>(IntStream.range(0, inventory.length()).map(actualSlotConvertor).max().orElse(0))
        for (i in 0 until inventory.length()) {
            itemStackArray[actualSlotConvertor.applyAsInt(i)] = getItem(i)
        }
        val itemStacks = listOf(*itemStackArray)
        for ((key, value) in viewers) {
            try {
                val packet = PacketPlayOutWindowItems(value, 0, itemStacks, ItemStack.AIR)
                key.clientConnection.sendPacket(packet)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    override fun getLocation(): Location? {
        return holder?.location
    }

    override val size: Int
        get() = inventory.length()

    override fun getMaxStackSize(): Int {
        return maxStackSize
    }

    override fun setMaxStackSize(size: Int) {
        this.maxStackSize = size
    }

    override fun getItem(index: Int): ItemStack? {
        return inventory.get(index)
    }

    override fun setItem(index: Int, item: ItemStack?) {
        var newItem = item
        if (newItem != null && newItem.type() == ItemStack.AIR.type()) {
            newItem = null
        }
        val oldItem = getItem(index)
        if (newItem != oldItem) {
            inventory.set(index, newItem)
            listener.slotChanged(this, index, oldItem, newItem)
        }
    }

    fun firstPartial(material: Key): Int {
        for (i in 0 until inventory.length()) {
            val item = getItem(i)
            if (item != null && item.type() == material && item.amount() < item.getMaxStackSize()) {
                return i
            }
        }
        return -1
    }

    private fun firstPartial(item: ItemStack?): Int {
        if (item == null) {
            return -1
        }
        for (i in 0 until inventory.length()) {
            val cItem = getItem(i)
            if (cItem != null && cItem.amount() < cItem.getMaxStackSize() && cItem.isSimilar(item)) {
                return i
            }
        }
        return -1
    }

    override fun addItem(vararg items: ItemStack): HashMap<Int, ItemStack> {
        val leftover = HashMap<Int, ItemStack>()
        for (i in items.indices) {
            var item = items[i]
            while (true) {
                val firstPartial = firstPartial(item)
                if (firstPartial == -1) {
                    val firstFree = firstEmpty()
                    if (firstFree == -1) {
                        leftover[i] = item
                        break
                    } else {
                        if (item.amount() > getMaxStackSize()) {
                            var stack = item.clone()
                            stack = stack.amount(getMaxStackSize())
                            setItem(firstFree, stack)
                            item = item.amount(item.amount() - getMaxStackSize())
                        } else {
                            setItem(firstFree, item)
                            break
                        }
                    }
                } else {
                    var partialItem = getItem(firstPartial)!!
                    val amount = item.amount()
                    val partialAmount = partialItem.amount()
                    val maxAmount = partialItem.getMaxStackSize()
                    if (amount + partialAmount <= maxAmount) {
                        partialItem = partialItem.amount(amount + partialAmount)
                        setItem(firstPartial, partialItem)
                        break
                    }
                    partialItem = partialItem.amount(maxAmount)
                    setItem(firstPartial, partialItem)
                    item = item.amount(amount + partialAmount - maxAmount)
                }
            }
        }
        return leftover
    }

    override fun removeItem(vararg items: ItemStack): HashMap<Int, ItemStack> {
        val leftover = HashMap<Int, ItemStack>()
        for (i in items.indices) {
            var item = items[i]
            var toDelete = item.amount()
            while (true) {
                val first = first(item, false)
                if (first == -1) {
                    item = item.amount(toDelete)
                    leftover[i] = item
                    break
                } else {
                    var itemStack = getItem(first)!!
                    val amount = itemStack.amount()
                    if (amount <= toDelete) {
                        toDelete -= amount
                        clear(first)
                    } else {
                        itemStack = itemStack.amount(amount - toDelete)
                        setItem(first, itemStack)
                        toDelete = 0
                    }
                }
                if (toDelete <= 0) {
                    break
                }
            }
        }
        return leftover
    }

    override fun getContents(): Array<ItemStack?> {
        return StreamSupport.stream(spliterator(), false).toArray { size -> arrayOfNulls<ItemStack>(size) }
    }

    override fun setContents(items: Array<ItemStack?>) {
        require(size >= items.size) { "Invalid inventory size; expected $size or less" }
        for (i in 0 until size) {
            if (i >= items.size) {
                setItem(i, null)
            } else {
                setItem(i, items[i])
            }
        }
    }

    override fun getStorageContents(): Array<ItemStack?> {
        return getContents()
    }

    override fun setStorageContents(items: Array<ItemStack?>) {
        setContents(items)
    }

    override fun contains(material: Key): Boolean {
        for (i in 0 until inventory.length()) {
            val itemStack = getItem(i)
            if (itemStack != null && itemStack.type() == material) {
                return true
            }
        }
        return false
    }

    override fun contains(item: ItemStack?): Boolean {
        for (i in 0 until inventory.length()) {
            val itemStack = getItem(i)
            if (itemStack == item) {
                return true
            }
        }
        return false
    }

    override fun contains(material: Key, amount: Int): Boolean {
        var remainingAmount = amount
        if (remainingAmount <= 0) {
            return true
        }
        for (i in 0 until inventory.length()) {
            val itemStack = getItem(i)
            if (itemStack != null && itemStack.type() == material) {
                remainingAmount -= itemStack.amount()
                if (remainingAmount <= 0) {
                    return true
                }
            }
        }
        return false
    }

    override fun contains(item: ItemStack?, amount: Int): Boolean {
        var remainingAmount = amount
        if (item == null) {
            return false
        }
        if (remainingAmount <= 0) {
            return true
        }
        for (i in 0 until inventory.length()) {
            val itemStack = getItem(i)
            if (item == itemStack && --remainingAmount <= 0) {
                return true
            }
        }
        return false
    }

    override fun containsAtLeast(item: ItemStack?, amount: Int): Boolean {
        var remainingAmount = amount
        if (item == null) {
            return false
        }
        if (remainingAmount <= 0) {
            return true
        }
        for (i in 0 until inventory.length()) {
            val itemStack = getItem(i)
            if (item.isSimilar(itemStack)) {
                remainingAmount -= itemStack!!.amount()
                if (remainingAmount <= 0) {
                    return true
                }
            }
        }
        return false
    }

    override fun all(material: Key): HashMap<Int, out ItemStack> {
        val slots = HashMap<Int, ItemStack>()
        val contents = getStorageContents()
        for (i in contents.indices) {
            val item = contents[i]
            if (item != null && item.type() == material) {
                slots[i] = item
            }
        }
        return slots
    }

    override fun all(item: ItemStack?): HashMap<Int, out ItemStack> {
        val slots = HashMap<Int, ItemStack>()
        if (item != null) {
            val contents = getStorageContents()
            for (i in contents.indices) {
                if (item == contents[i]) {
                    slots[i] = contents[i]!!
                }
            }
        }
        return slots
    }

    override fun first(material: Key): Int {
        for (i in 0 until inventory.length()) {
            val item = getItem(i)
            if (item != null && item.type() == material) {
                return i
            }
        }
        return -1
    }

    override fun first(item: ItemStack?): Int {
        return first(item, true)
    }

    private fun first(item: ItemStack?, withAmount: Boolean): Int {
        if (item == null) {
            return -1
        }
        for (i in 0 until inventory.length()) {
            val itemStack = inventory.get(i) ?: continue
            if (if (withAmount) item == itemStack else item.isSimilar(itemStack)) {
                return i
            }
        }
        return -1
    }

    override fun firstEmpty(): Int {
        for (i in 0 until inventory.length()) {
            if (getItem(i) == null) {
                return i
            }
        }
        return -1
    }

    override fun isEmpty(): Boolean {
        for (i in 0 until inventory.length()) {
            if (getItem(i) != null) {
                return false
            }
        }
        return true
    }

    override fun remove(material: Key) {
        for (i in 0 until inventory.length()) {
            val itemStack = getItem(i)
            if (itemStack != null && itemStack.type() == material) {
                clear(i)
            }
        }
    }

    override fun remove(item: ItemStack?) {
        for (i in 0 until inventory.length()) {
            val itemStack = getItem(i)
            if (itemStack != null && itemStack == item) {
                clear(i)
            }
        }
    }

    override fun clear(index: Int) {
        setItem(index, null)
    }

    override fun clear() {
        for (i in 0 until inventory.length()) {
            setItem(i, null)
        }
    }

    override fun getViewers(): Set<Player> {
        return Collections.unmodifiableSet(viewers.keys)
    }

    override fun iterator(): MutableIterator<ItemStack?> {
        return InventoryIterator(this)
    }

    override fun iterator(index: Int): MutableIterator<ItemStack?> {
        var actualIndex = index
        if (actualIndex < 0) {
            actualIndex += size + 1
        }
        return InventoryIterator(this, actualIndex)
    }

    @Deprecated("")
    override fun getUnsafe(): Inventory.Unsafe {
        return unsafeImpl
    }

    @Deprecated("")
    class Unsafe @Deprecated("") constructor(private val inventory: AbstractInventory) : Inventory.Unsafe {

        @Deprecated("")
        override fun a(index: Int, itemStack: ItemStack?) {
            var newItem = itemStack
            if (newItem != null && newItem.type() == ItemStack.AIR.type()) {
                newItem = null
            }
            inventory.inventory.set(index, newItem)
        }

        @Deprecated("")
        override fun b(index: Int, itemStack: ItemStack?) {
            inventory.inventory.set(inventory.actualInverseSlotConvertor.applyAsInt(index), itemStack)
        }

        @Deprecated("")
        override fun a(): IntUnaryOperator {
            return inventory.actualSlotConvertor
        }

        @Deprecated("")
        override fun b(): IntUnaryOperator {
            return inventory.actualInverseSlotConvertor
        }

        override fun c(): MutableMap<Player, Int> {
            return inventory.viewers
        }
    }
}

