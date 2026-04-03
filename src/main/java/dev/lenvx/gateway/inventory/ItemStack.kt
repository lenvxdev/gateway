package dev.lenvx.gateway.inventory

import dev.lenvx.gateway.registry.DataComponentType
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.querz.nbt.tag.Tag
import java.util.*

class ItemStack @JvmOverloads constructor(
    private val material: Key,
    private val amount: Int = 1,
    private val components: Map<Key, Tag<*>> = emptyMap()
) : Cloneable {

    public override fun clone(): ItemStack {
        return ItemStack(material, amount, components)
    }

    fun type(): Key = material

    fun type(material: Key): ItemStack {
        return ItemStack(material, amount, components)
    }

    fun amount(): Int = amount

    fun amount(amount: Int): ItemStack {
        return ItemStack(material, amount, components)
    }

    fun components(): Map<Key, Tag<*>> {
        return HashMap(components)
    }

    fun components(components: Map<Key, Tag<*>>): ItemStack {
        return ItemStack(material, amount, components)
    }

    fun <T> getComponent(type: DataComponentType<T>): T? {
        val tag = components[type.key] ?: return null
        return type.codec.decode(tag)
    }

    fun <T> setComponent(type: DataComponentType<T>, value: T): ItemStack {
        val newComponents = components().toMutableMap()
        newComponents[type.key] = type.codec.encode(value) as Tag<*>
        return components(newComponents)
    }

    fun displayName(): Component? {
        if (type() == AIR.type()) {
            return null
        }
        return try {
            getComponent(DataComponentType.CUSTOM_NAME)
        } catch (e: Exception) {
            null
        }
    }

    fun displayName(component: Component?): ItemStack {
        if (type() == AIR.type()) {
            return this
        }
        return if (component == null) {
            val newComponents = components().toMutableMap()
            newComponents.remove(DataComponentType.CUSTOM_NAME.key)
            components(newComponents)
        } else {
            setComponent(DataComponentType.CUSTOM_NAME, component)
        }
    }

    fun getMaxStackSize(): Int = 64

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val itemStack = other as ItemStack
        return amount == itemStack.amount && material == itemStack.material && components == itemStack.components
    }

    fun isSimilar(stack: ItemStack?): Boolean {
        return stack != null && material == stack.material && components == stack.components
    }

    override fun hashCode(): Int {
        return Objects.hash(material, amount, components)
    }

    override fun toString(): String {
        return "ItemStack{" +
                "material=" + material +
                ", amount=" + amount +
                ", components=" + components +
                '}'
    }

    companion object {
        @JvmField
        val AIR = ItemStack(Key.key("minecraft:air"), 0)
    }
}

