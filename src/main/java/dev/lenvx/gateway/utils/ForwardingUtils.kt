package dev.lenvx.gateway.utils

import dev.lenvx.gateway.Gateway
import net.kyori.adventure.key.Key
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object ForwardingUtils {

	@JvmField
	val VELOCITY_FORWARDING_CHANNEL = Key.key("velocity", "player_info")

	@JvmStatic
	@Throws(IOException::class)
	fun validateVelocityModernResponse(data: ByteArray): Boolean {
		val input = DataInputStream(ByteArrayInputStream(data))
		val signature = ByteArray(32)
		input.readFully(signature)

		var foundValid = false
		try {
			val mac = Mac.getInstance("HmacSHA256")
			val secrets = Gateway.instance?.serverProperties?.forwardingSecrets ?: emptyList()
			for (secret in secrets) {
				val key = SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256")
				mac.init(key)
				mac.update(data, 32, data.size - 32)
				val sig = mac.doFinal()
				if (MessageDigest.isEqual(signature, sig)) {
					foundValid = true
					break
				}
			}
		} catch (e: InvalidKeyException) {
			throw RuntimeException("Unable to authenticate data", e)
		} catch (e: NoSuchAlgorithmException) {
			throw AssertionError(e)
		}

		return foundValid
	}

	@JvmStatic
	@Throws(IOException::class)
	fun getVelocityDataFrom(data: ByteArray): VelocityModernForwardingData {
		val input = DataInputStream(ByteArrayInputStream(data))
		val signature = ByteArray(32)
		input.readFully(signature)

		val velocityVersion = DataTypeIO.readVarInt(input)
		if (velocityVersion != 1) {
			println("Unsupported Velocity version! Stopping Execution")
			throw AssertionError("Unknown Velocity Packet")
		}
		val address = DataTypeIO.readString(input, StandardCharsets.UTF_8)
		val uuid = DataTypeIO.readUUID(input)
		val username = DataTypeIO.readString(input, StandardCharsets.UTF_8)

		var skinResponse: MojangAPIUtils.SkinResponse? = null
		val count = DataTypeIO.readVarInt(input)
		for (i in 0 until count) {
			val propertyName = DataTypeIO.readString(input, StandardCharsets.UTF_8)
			val propertyValue = DataTypeIO.readString(input, StandardCharsets.UTF_8)
			var propertySignature = ""
			val signatureIncluded = input.readBoolean()
			if (signatureIncluded) {
				propertySignature = DataTypeIO.readString(input, StandardCharsets.UTF_8)
			}
			if (propertyName == "textures") {
				skinResponse = MojangAPIUtils.SkinResponse(propertyValue, propertySignature)
				break
			}
		}

		return VelocityModernForwardingData(velocityVersion, address, uuid, username, skinResponse)
	}

	data class VelocityModernForwardingData(
		val version: Int,
		val ipAddress: String,
		val uuid: UUID,
		val username: String,
		val skinResponse: MojangAPIUtils.SkinResponse?
	)
}


