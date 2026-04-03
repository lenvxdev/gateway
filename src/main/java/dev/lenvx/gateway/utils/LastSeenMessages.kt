package dev.lenvx.gateway.utils

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.BitSet
import java.util.Collections

class LastSeenMessages(
    private val entries: MutableList<MessageSignature>
) {

    class a(
        private val entries: MutableList<MessageSignature.a>
    ) {

        @Throws(IOException::class)
        constructor(input: DataInputStream) : this(ArrayList()) {
            val size = DataTypeIO.readVarInt(input)
            for (i in 0 until size) {
                entries.add(MessageSignature.a.read(input))
            }
        }

        @Throws(IOException::class)
        fun write(out: DataOutputStream) {
            DataTypeIO.writeVarInt(out, entries.size)
            for (entry in entries) {
                MessageSignature.a.write(out, entry)
            }
        }

        companion object {
            @JvmField
            val EMPTY: a = a(Collections.emptyList<MessageSignature.a>().toMutableList())
        }
    }

    class b(
        private val offset: Int,
        private val acknowledged: BitSet,
        private val checksum: Byte
    ) {

        @Throws(IOException::class)
        constructor(input: DataInputStream) : this(
            DataTypeIO.readVarInt(input),
            DataTypeIO.readFixedBitSet(input, 20),
            input.readByte()
        )

        @Throws(IOException::class)
        fun write(out: DataOutputStream) {
            DataTypeIO.writeVarInt(out, offset)
            DataTypeIO.writeFixedBitSet(out, acknowledged, 20)
            out.writeByte(checksum.toInt())
        }
    }

    companion object {
        @JvmField
        val EMPTY: ArgumentSignatures = ArgumentSignatures(Collections.emptyList<ArgumentSignatures.a>().toMutableList())
        private const val MAX_ARGUMENT_COUNT: Int = 8
        private const val MAX_ARGUMENT_NAME_LENGTH: Int = 16
    }
}
