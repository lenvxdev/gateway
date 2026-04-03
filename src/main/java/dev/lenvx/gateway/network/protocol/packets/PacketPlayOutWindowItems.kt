package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.inventory.ItemStack
import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class PacketPlayOutWindowItems(
    val containerId: Int,
    val stateId: Int,
    val items: List<ItemStack?>,
    val carriedItem: ItemStack?
) : PacketOut() {

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))

        DataTypeIO.writeVarInt(output, containerId)
        DataTypeIO.writeVarInt(output, stateId)
        DataTypeIO.writeVarInt(output, items.size)
        for (itemStack in items) {
            DataTypeIO.writeItemStack(output, itemStack)
        }
        DataTypeIO.writeItemStack(output, carriedItem)

        return buffer.toByteArray()
    }
}
