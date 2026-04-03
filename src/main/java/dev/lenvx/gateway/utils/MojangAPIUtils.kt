package dev.lenvx.gateway.utils

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.UUID
import java.util.stream.Collectors
import javax.net.ssl.HttpsURLConnection

object MojangAPIUtils {

	data class SkinResponse(val skin: String, val signature: String)

	@JvmStatic
	fun getOnlineUUIDOfPlayerFromMojang(username: String): UUID? {
		try {
			val url = URL("https://api.mojang.com/users/profiles/minecraft/$username")
			val connection = url.openConnection() as HttpsURLConnection
			connection.useCaches = false
			connection.defaultUseCaches = false
			connection.addRequestProperty("User-Agent", "Mozilla/5.0")
			connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
			connection.addRequestProperty("Pragma", "no-cache")
			if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
				BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
					val reply = reader.lines().collect(Collectors.joining()).replace(" ", "")
					if (!reply.contains("\"error\":\"BadRequestException\"")) {
						val uuidStr = reply.split("\"id\":\"")[1].split("\"")[0]
						return UUID.fromString(uuidStr.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)".toRegex(), "$1-$2-$3-$4-$5"))
					}
				}
			} else {
				System.err.println("Connection could not be opened (Response code ${connection.responseCode}, ${connection.responseMessage})")
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

	@JvmStatic
	fun getSkinFromMojangServer(username: String): SkinResponse? {
		val uuid = getOnlineUUIDOfPlayerFromMojang(username) ?: return null
		return getSkinFromMojangServer(uuid)
	}

	@JvmStatic
	fun getSkinFromMojangServer(uuid: UUID): SkinResponse? {
		try {
			val url = URL("https://sessionserver.mojang.com/session/minecraft/profile/$uuid?unsigned=false")
			val connection = url.openConnection() as HttpsURLConnection
			connection.useCaches = false
			connection.defaultUseCaches = false
			connection.addRequestProperty("User-Agent", "Mozilla/5.0")
			connection.addRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
			connection.addRequestProperty("Pragma", "no-cache")
			if (connection.responseCode == HttpsURLConnection.HTTP_OK) {
				BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
					val reply = reader.lines().collect(Collectors.joining("")).replace(" ", "")
					val skin = reply.split("\"value\":\"")[1].split("\"")[0]
					val signature = reply.split("\"signature\":\"")[1].split("\"")[0]
					return SkinResponse(skin, signature)
				}
			} else {
				System.err.println("Connection could not be opened (Response code ${connection.responseCode}, ${connection.responseMessage})")
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return null
	}

}

