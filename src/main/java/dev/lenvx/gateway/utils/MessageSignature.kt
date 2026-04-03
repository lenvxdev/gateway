package dev.lenvx.gateway.utils

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Arrays
import java.util.Base64

class MessageSignature(
    private val bytes: ByteArray
) {

    fun asByteBuffer(): ByteBuffer {
        return ByteBuffer.wrap(bytes)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is MessageSignature) {
            return false
        }
        return Arrays.equals(bytes, other.bytes)
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(bytes)
    }

    override fun toString(): String {
        return Base64.getEncoder().encodeToString(bytes)
    }

    class a(
        private val id: Int,
        private val fullSignature: MessageSignature?
    ) {

        constructor(messagesignature: MessageSignature) : this(-1, messagesignature)

        constructor(i: Int) : this(i, null)

        fun id(): Int = id

        fun fullSignature(): MessageSignature? = fullSignature

        companion object {
            const val FULL_SIGNATURE: Int = -1

            @JvmStatic
            @Throws(IOException::class)
            fun read(input: DataInputStream): a {
                val i = DataTypeIO.readVarInt(input) - 1
                return if (i == -1) a(MessageSignature.read(input)) else a(i)
            }

            @JvmStatic
            @Throws(IOException::class)
            fun write(out: DataOutputStream, signature: a) {
                DataTypeIO.writeVarInt(out, signature.id() + 1)
                if (signature.fullSignature() != null) {
                    MessageSignature.write(out, signature.fullSignature()!!)
                }
            }
        }
    }

    companion object {
        const val BYTES: Int = 256

        @JvmStatic
        @Throws(IOException::class)
        fun read(input: DataInputStream): MessageSignature {
            val bytes = ByteArray(BYTES)
            input.readFully(bytes)
            return MessageSignature(bytes)
        }

        @JvmStatic
        @Throws(IOException::class)
        fun write(out: DataOutputStream, signature: MessageSignature) {
            out.write(signature.bytes)
        }
    }
}
