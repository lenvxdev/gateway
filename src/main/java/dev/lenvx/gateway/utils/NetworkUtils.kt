package dev.lenvx.gateway.utils

import java.io.IOException
import java.net.DatagramSocket
import java.net.ServerSocket

object NetworkUtils {

	@JvmStatic
	fun available(port: Int): Boolean {
		var ss: ServerSocket? = null
		var ds: DatagramSocket? = null
		try {
			ss = ServerSocket(port)
			ss.reuseAddress = true
			ds = DatagramSocket(port)
			ds.reuseAddress = true
			return true
		} catch (e: IOException) {
			e.printStackTrace()
		} finally {
			ds?.close()
			try {
				ss?.close()
			} catch (e: IOException) {
				e.printStackTrace()
			}
		}
		return false
	}
}

