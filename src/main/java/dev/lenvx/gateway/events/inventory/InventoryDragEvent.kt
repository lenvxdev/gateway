package dev.lenvx.gateway.events.inventory

import dev.lenvx.gateway.events.Cancellable
import dev.lenvx.gateway.inventory.DragType
import dev.lenvx.gateway.inventory.InventoryView
import dev.lenvx.gateway.inventory.ItemStack
import java.util.*

class InventoryDragEvent(
	view: InventoryView,
	var carriedItem: ItemStack?,
	private val oldCarriedItemValue: ItemStack,
	isSingle: Boolean,
	slots: Map<Int, ItemStack>
) : InventoryEvent(view, slots.keys.firstOrNull()?.let { view.getInventory(it) }), Cancellable {
	val type: DragType = if (isSingle) DragType.SINGLE else DragType.EVEN
	val newItems: Map<Int, ItemStack> = Collections.unmodifiableMap(slots)
	val rawSlots: Set<Int> get() = newItems.keys
	val inventorySlots: Set<Int> = Collections.unmodifiableSet(slots.keys.map { view.convertSlot(it) }.toSet())

	fun getOldCarriedItem(): ItemStack = oldCarriedItemValue.clone()

	override var isCancelled: Boolean = false

}

