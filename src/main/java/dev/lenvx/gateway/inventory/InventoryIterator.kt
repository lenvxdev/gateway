package dev.lenvx.gateway.inventory

class InventoryIterator @JvmOverloads constructor(
    private val inventory: Inventory,
    private var nextIndex: Int = 0
) : MutableListIterator<ItemStack?> {

    private var lastDirection: Boolean? = null

    override fun hasNext(): Boolean {
        return nextIndex < inventory.size
    }

    override fun next(): ItemStack? {
        lastDirection = true
        return inventory.getItem(nextIndex++)
    }

    override fun nextIndex(): Int {
        return nextIndex
    }

    override fun hasPrevious(): Boolean {
        return nextIndex > 0
    }

    override fun previous(): ItemStack? {
        lastDirection = false
        return inventory.getItem(--nextIndex)
    }

    override fun previousIndex(): Int {
        return nextIndex - 1
    }

    override fun set(element: ItemStack?) {
        val direction = lastDirection ?: throw IllegalStateException("No current item!")
        val i = if (direction) nextIndex - 1 else nextIndex
        inventory.setItem(i, element)
    }

    override fun add(element: ItemStack?) {
        throw UnsupportedOperationException("Can't change the size of an inventory!")
    }

    override fun remove() {
        throw UnsupportedOperationException("Can't change the size of an inventory!")
    }
}


