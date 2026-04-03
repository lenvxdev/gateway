package dev.lenvx.gateway.network

import dev.lenvx.gateway.Gateway
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ServerConnection(val ip: String, val port: Int, val isSilent: Boolean) : Thread() {

	val clients: MutableList<ClientConnection> = CopyOnWriteArrayList()
	private val executorService: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
	var serverSocket: ServerSocket? = null
		private set

	init {
		start()
	}

	override fun run() {
		try {
			serverSocket = ServerSocket(port, 50, InetAddress.getByName(ip))
			if (!isSilent) {
				Gateway.instance?.console?.sendMessage("Gateway server listening on /" + serverSocket!!.inetAddress.hostName + ":" + serverSocket!!.localPort)
			}
			while (true) {
				val connection = serverSocket!!.accept()
				val clientTask = ClientConnection(connection)
				clients.add(clientTask)
				executorService.submit(clientTask)
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

    fun getClientsList(): List<ClientConnection> {
        return clients
    }

}


