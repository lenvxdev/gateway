package dev.lenvx.gateway.network.protocol.packets

import dev.lenvx.gateway.inventory.ItemStack
import dev.lenvx.gateway.registry.PacketRegistry
import dev.lenvx.gateway.utils.DataTypeIO
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException

class PacketPlayOutSetSlot(
    val containerId: Int,
    val stateId: Int,
    val slot: Int,
    itemStack: ItemStack?
) : PacketOut() {

    val itemStack: ItemStack = itemStack ?: ItemStack.AIR

    @Throws(IOException::class)
    override fun serializePacket(): ByteArray {
        val buffer = ByteArrayOutputStream()
        val output = DataOutputStream(buffer)
        DataTypeIO.writeVarInt(output, PacketRegistry.getPacketId(this::class.java))

        DataTypeIO.writeVarInt(output, containerId)
        DataTypeIO.writeVarInt(output, stateId)
        output.writeShort(slot)
        DataTypeIO.writeItemStack(output, itemStack)

        return buffer.toByteArray()
    }
}
