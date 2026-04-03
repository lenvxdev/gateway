package dev.lenvx.gateway.utils

import com.google.gson.JsonElement
import dev.lenvx.gateway.inventory.ItemStack
import dev.lenvx.gateway.location.BlockFace
import dev.lenvx.gateway.location.MovingObjectPositionBlock
import dev.lenvx.gateway.location.Vector
import dev.lenvx.gateway.registry.BuiltInRegistries
import dev.lenvx.gateway.registry.DataComponentType
import dev.lenvx.gateway.world.BlockPosition
import dev.lenvx.gateway.world.ChunkPosition
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.querz.nbt.io.NBTInputStream
import net.querz.nbt.io.NBTOutputStream
import net.querz.nbt.tag.EndTag
import net.querz.nbt.tag.Tag

import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.PushbackInputStream
import java.nio.charset.Charset
import java.util.Arrays
import java.util.BitSet
import java.util.EnumSet
import java.util.HashMap
import java.util.UUID

object DataTypeIO {

	const val MAX_STRING_LENGTH = 32767

	@JvmStatic
	@Throws(IOException::class)
	fun writeItemStack(out: DataOutputStream, itemstack: ItemStack?) {
		if (itemstack == null || itemstack.isSimilar(ItemStack.AIR) || itemstack.amount() == 0) {
			writeVarInt(out, 0)
		} else {
			writeVarInt(out, itemstack.amount())
			writeVarInt(out, BuiltInRegistries.ITEM_REGISTRY.getId(itemstack.type()))
			val components = itemstack.components()
			val buffer = ByteArrayOutputStream()
			val componentOut = DataOutputStream(buffer)
			var componentSize = 0
			for (entry in components.entries) {
				val componentKey = entry.key
				val typeId = BuiltInRegistries.DATA_COMPONENT_TYPE.getId(componentKey)
				if (typeId >= 0 && DataComponentType.isKnownType(componentKey)) {
					writeVarInt(componentOut, typeId)
					writeTag(componentOut, entry.value)
					componentSize++
				}
			}
			writeVarInt(out, componentSize)
			writeVarInt(out, 0)
			out.write(buffer.toByteArray())
		}
	}

	@JvmStatic
	@Throws(IOException::class)
	fun readUntrustedItemStack(inStream: DataInputStream): ItemStack {
		val amount = readVarInt(inStream)
		return if (amount <= 0) {
			ItemStack.AIR
		} else {
			val key = BuiltInRegistries.ITEM_REGISTRY.fromId(readVarInt(inStream))
                ?: throw IOException("Unknown item registry id while reading untrusted item stack")
			val size = readVarInt(inStream)
			val removeSize = readVarInt(inStream)
			val components = HashMap<Key, Tag<*>>()
			for (i in 0 until size) {
				val componentKey = BuiltInRegistries.DATA_COMPONENT_TYPE.fromId(readVarInt(inStream))
				readVarInt(inStream)
				val component = readTag(inStream, Tag::class.java)
				if (componentKey != null && DataComponentType.isKnownType(componentKey)) {
					components[componentKey] = component!!
				}
			}
			for (i in 0 until removeSize) {
				readVarInt(inStream)
			}
				ItemStack(key, amount, components)
		}
	}

    @JvmStatic
	@Throws(IOException::class)
    fun readItemStack(inStream: DataInputStream): ItemStack {
		val amount = readVarInt(inStream)
		return if (amount <= 0) {
			ItemStack.AIR
		} else {
			val key = BuiltInRegistries.ITEM_REGISTRY.fromId(readVarInt(inStream))
                ?: throw IOException("Unknown item registry id while reading item stack")
			val size = readVarInt(inStream)
			val removeSize = readVarInt(inStream)
			val components = HashMap<Key, Tag<*>>()
			for (i in 0 until size) {
				val componentKey = BuiltInRegistries.DATA_COMPONENT_TYPE.fromId(readVarInt(inStream))
				val component = readTag(inStream, Tag::class.java)
				if (componentKey != null && DataComponentType.isKnownType(componentKey)) {
					components[componentKey] = component!!
				}
			}
			for (i in 0 until removeSize) {
				readVarInt(inStream)
			}
				ItemStack(key, amount, components)
		}
	}

	@JvmStatic
	@Throws(IOException::class)
	fun writeBlockHitResult(out: DataOutputStream, movingobjectpositionblock: MovingObjectPositionBlock) {
		val blockposition = movingobjectpositionblock.blockPos

		writeBlockPosition(out, blockposition)
		writeVarInt(out, movingobjectpositionblock.direction.ordinal)
		val vector = movingobjectpositionblock.location

		out.writeFloat((vector.x - blockposition.x.toDouble()).toFloat())
		out.writeFloat((vector.y - blockposition.y.toDouble()).toFloat())
		out.writeFloat((vector.z - blockposition.z.toDouble()).toFloat())
		out.writeBoolean(movingobjectpositionblock.isInside)
		out.writeBoolean(movingobjectpositionblock.isWorldBorderHit)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun readBlockHitResult(inStream: DataInputStream): MovingObjectPositionBlock {
		val blockposition = readBlockPosition(inStream)
		val direction = BlockFace.values()[readVarInt(inStream)]
		val f = inStream.readFloat()
		val f1 = inStream.readFloat()
		val f2 = inStream.readFloat()
		val flag = inStream.readBoolean()
		val flag1 = inStream.readBoolean()

		return MovingObjectPositionBlock(Vector(blockposition.x.toDouble() + f.toDouble(), blockposition.y.toDouble() + f1.toDouble(), blockposition.z.toDouble() + f2.toDouble()), direction, blockposition, flag, flag1)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun <E : Enum<E>> writeEnumSet(out: DataOutputStream, enumset: EnumSet<E>, oclass: Class<E>) {
		val ae = oclass.enumConstants
		val bitset = BitSet(ae.size)

		for (i in ae.indices) {
			bitset.set(i, enumset.contains(ae[i]))
		}

		writeFixedBitSet(out, bitset, ae.size)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun <E : Enum<E>> readEnumSet(inStream: DataInputStream, oclass: Class<E>): EnumSet<E> {
		val ae = oclass.enumConstants
		val bitset = readFixedBitSet(inStream, ae.size)
		val enumset = EnumSet.noneOf(oclass)

		for (i in ae.indices) {
			if (bitset.get(i)) {
				enumset.add(ae[i])
			}
		}

		return enumset
	}

	@JvmStatic
	@Throws(IOException::class)
	fun writeFixedBitSet(out: DataOutputStream, bitset: BitSet, i: Int) {
		if (bitset.length() > i) {
			val j = bitset.length()
			throw RuntimeException("BitSet is larger than expected size ($j>$i)")
		} else {
			val abyte = bitset.toByteArray()
			out.write(Arrays.copyOf(abyte, -Math.floorDiv(-i, 8)))
		}
	}

	@JvmStatic
	@Throws(IOException::class)
	fun readFixedBitSet(inStream: DataInputStream, i: Int): BitSet {
		val abyte = ByteArray(-Math.floorDiv(-i, 8))
		inStream.readFully(abyte)
		return BitSet.valueOf(abyte)
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun writeBlockPosition(out: DataOutputStream, position: BlockPosition) {
        out.writeLong(((position.x.toLong() and 0x3FFFFFFL) shl 38) or ((position.z.toLong() and 0x3FFFFFFL) shl 12) or (position.y.toLong() and 0xFFFL))
	}

	@JvmStatic
	@Throws(IOException::class)
	fun readBlockPosition(inStream: DataInputStream): BlockPosition {
		val value = inStream.readLong()
		val x = (value shr 38).toInt()
		val y = (value shl 52 shr 52).toInt()
		val z = (value shl 26 shr 38).toInt()
		return BlockPosition(x, y, z)
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun writeUUID(out: DataOutputStream, uuid: UUID) {
		out.writeLong(uuid.mostSignificantBits)
		out.writeLong(uuid.leastSignificantBits)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun readUUID(inStream: DataInputStream): UUID {
		return UUID(inStream.readLong(), inStream.readLong())
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun writeTag(out: DataOutputStream, tag: Tag<*>?) {
		var t = tag
		if (t == null) {
			t = EndTag.INSTANCE
		}
		out.writeByte(t!!.id.toInt())
		if (t.id != EndTag.ID) {
			NBTOutputStream(out).writeRawTag(t, Tag.DEFAULT_MAX_DEPTH)
		}
	}

	@JvmStatic
	@Suppress("UNCHECKED_CAST")
	@Throws(IOException::class)
	fun <T : Tag<*>> readTag(inStream: DataInputStream, type: Class<T>): T? {
		val b = inStream.readByte()
		if (b == EndTag.ID) {
			return if (type.isInstance(EndTag.INSTANCE)) EndTag.INSTANCE as T else null
		}
		val buffered = PushbackInputStream(inStream)
		buffered.unread(b.toInt())
		return NBTInputStream(buffered).readRawTag(Tag.DEFAULT_MAX_DEPTH) as T?
	}

	@JvmStatic
	@Throws(IOException::class)
	fun readString(inStream: DataInputStream, charset: Charset): String {
		val length = readVarInt(inStream)

	    if (length == -1) {
	        throw IOException("Premature end of stream.")
	    }
		if (length > MAX_STRING_LENGTH) {
			throw IOException("The received string length is longer than maximum allowed ($length > $MAX_STRING_LENGTH)")
		}

	    val b = ByteArray(length)
	    inStream.readFully(b)
	    return String(b, charset)
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun getStringLength(string: String, charset: Charset): Int {
	    val bytes = string.toByteArray(charset)
	    return getVarIntLength(bytes.size) + bytes.size
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun writeString(out: DataOutputStream, string: String, charset: Charset) {
	    val bytes = string.toByteArray(charset)
	    writeVarInt(out, bytes.size)
	    out.write(bytes)
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun readVarInt(inStream: DataInputStream): Int {
		var i = 0
		var j = 0
		var b: Byte
		do {
			b = inStream.readByte()
			i = i or ((b.toInt() and 127) shl j++ * 7)
			if (j > 5) {
				throw RuntimeException("VarInt too big")
			}
		} while ((b.toInt() and 128) == 128)
		return i
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun writeVarInt(out: DataOutputStream, value: Int) {
		var v = value
		while ((v and -128) != 0) {
			out.writeByte(v and 127 or 128)
			v = v ushr 7
		}
		out.writeByte(v)
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun getVarIntLength(value: Int): Int {
		return when {
			(value and -0x80) == 0 -> 1
			(value and -0x4000) == 0 -> 2
			(value and -0x200000) == 0 -> 3
			(value and -0x10000000) == 0 -> 4
			else -> 5
		}
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun readVarLong(inStream: DataInputStream): Long {
	    var numRead = 0
	    var result: Long = 0
	    var read: Byte
	    do {
	        read = inStream.readByte()
	        val value = (read.toInt() and 0b01111111).toLong()
	        result = result or (value shl (7 * numRead))

	        numRead++
	        if (numRead > 10) {
	            throw RuntimeException("VarLong is too big")
	        }
	    } while ((read.toInt() and 0b10000000) != 0)

	    return result
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun writeVarLong(out: DataOutputStream, value: Long) {
		var v = value
	    do {
	        var temp = (v and 0b01111111).toByte()
	        v = v ushr 7
	        if (v != 0L) {
	            temp = (temp.toInt() or 0b10000000).toByte()
	        }
	        out.writeByte(temp.toInt())
	    } while (v != 0L)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun readComponent(inStream: DataInputStream): Component {
		val tag = readTag(inStream, Tag::class.java)
		if (tag == null || tag is EndTag) {
			throw IllegalArgumentException("Got end-tag when trying to read Component")
		}
		val json = NbtComponentSerializer.tagComponentToJson(tag)
            ?: throw IOException("Failed to convert NBT component tag to json")
		return GsonComponentSerializer.gson().deserializeFromTree(json)
            ?: throw IOException("Failed to deserialize component from NBT json")
	}

	@JvmStatic
	@Throws(IOException::class)
	fun writeComponent(out: DataOutputStream, component: Component) {
		val json = GsonComponentSerializer.gson().serializeToTree(component)
		val tag = NbtComponentSerializer.jsonComponentToTag(json)
		writeTag(out, tag)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun writeJsonComponent(out: DataOutputStream, component: Component) {
		val json = GsonComponentSerializer.gson().serialize(component)
		writeString(out, json, Charsets.UTF_8)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun writeChunkPosition(out: DataOutputStream, chunkPosition: ChunkPosition) {
		val l = chunkPosition.chunkX.toLong() and 4294967295L or ((chunkPosition.chunkZ.toLong() and 4294967295L) shl 32)
		out.writeLong(l)
	}

	@JvmStatic
	@Throws(IOException::class)
	fun consumeHashedStack(inStream: DataInputStream) {
		if (inStream.readBoolean()) {
			readVarInt(inStream)
			readVarInt(inStream)
			val addedSize = readVarInt(inStream)
			for (i in 0 until addedSize) {
				readVarInt(inStream)
				inStream.readInt()
			}
			val removedSize = readVarInt(inStream)
			for (i in 0 until removedSize) {
				readVarInt(inStream)
			}
		}
	}

    private fun sanitize(d0: Double): Double {
        return if (java.lang.Double.isNaN(d0)) 0.0 else Math.min(Math.max(d0, -1.7179869183E10), 1.7179869183E10)
    }

    private fun pack(d0: Double): Long {
        return Math.round((d0 * 0.5 + 0.5) * 32766.0)
    }

	@JvmStatic
	@Throws(IOException::class)
    fun writeLpVec3(out: DataOutputStream, vec3d: Vector) {
        val d0 = sanitize(vec3d.x)
        val d1 = sanitize(vec3d.y)
        val d2 = sanitize(vec3d.z)
        val d3 = Math.max(Math.abs(d0), Math.max(Math.abs(d1), Math.abs(d2)))

        if (d3 < 3.051944088384301E-5) {
            out.writeByte(0)
        } else {
            val i = Math.ceil(d3).toLong()
            val flag = (i and 3L) != i
            val j = if (flag) i and 3L or 4L else i
            val k = pack(d0 / i.toDouble()) shl 3
            val l = pack(d1 / i.toDouble()) shl 18
            val i1 = pack(d2 / i.toDouble()) shl 33
            val j1 = j or k or l or i1

            out.writeByte(j1.toInt())
            out.writeByte((j1 shr 8).toInt())
            out.writeInt((j1 shr 16).toInt())
            if (flag) {
                writeVarInt(out, (i shr 2).toInt())
            }
        }
    }

}

