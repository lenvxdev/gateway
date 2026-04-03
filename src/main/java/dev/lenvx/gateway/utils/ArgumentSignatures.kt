package dev.lenvx.gateway.utils

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Collections

class ArgumentSignatures(
    private val entries: MutableList<a>
) {

    @Throws(IOException::class)
    constructor(input: DataInputStream) : this(ArrayList(8)) {
        val size = DataTypeIO.readVarInt(input)
        for (i in 0 until size) {
            entries.add(a(input))
        }
    }

    fun get(s: String): MessageSignature? {
        for (entry in entries) {
            if (entry.name == s) {
                return entry.signature
            }
        }
        return null
    }

    @Throws(IOException::class)
    fun write(out: DataOutputStream) {
        DataTypeIO.writeVarInt(out, entries.size)
        for (entry in entries) {
            entry.write(out)
        }
    }

    class a(
        val name: String,
        val signature: MessageSignature
    ) {

        @Throws(IOException::class)
        constructor(input: DataInputStream) : this(
            DataTypeIO.readString(input, StandardCharsets.UTF_8),
            MessageSignature.read(input)
        )

        @Throws(IOException::class)
        fun write(out: DataOutputStream) {
            DataTypeIO.writeString(out, name, StandardCharsets.UTF_8)
            MessageSignature.write(out, signature)
        }
    }

    fun interface b {
        fun sign(s: String): MessageSignature
    }

    companion object {
        @JvmField
        val EMPTY: ArgumentSignatures = ArgumentSignatures(Collections.emptyList<a>().toMutableList())
        private const val MAX_ARGUMENT_COUNT: Int = 8
        private const val MAX_ARGUMENT_NAME_LENGTH: Int = 16
    }
}
