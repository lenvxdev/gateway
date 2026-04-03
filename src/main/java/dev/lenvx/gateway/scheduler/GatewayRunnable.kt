package dev.lenvx.gateway.scheduler

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.plugins.GatewayPlugin

abstract class GatewayRunnable : GatewayTask {

	@Volatile
	var isRegistered: Boolean = false
		private set

	@Volatile
	private var _taskId: Int = -1

	val taskId: Int
		get() {
			if (isRegistered && _taskId >= 0) {
				return _taskId
			} else {
				throw IllegalStateException("GatewayRunnable not yet scheduled")
			}
		}

	fun cancel() {
		synchronized(this) {
			if (isRegistered && _taskId >= 0) {
				Gateway.getInstance().scheduler.cancelTask(_taskId)
			}
		}
	}

	fun runTask(plugin: GatewayPlugin): GatewayRunnable {
		synchronized(this) {
			if (!isRegistered) {
				_taskId = Gateway.getInstance().scheduler.runTask(plugin, this)
				isRegistered = true
				return this
			} else {
				throw IllegalStateException("GatewayRunnable already scheduled")
			}
		}
	}

	fun runTaskLater(plugin: GatewayPlugin, delay: Long): GatewayRunnable {
		synchronized(this) {
			if (!isRegistered) {
				_taskId = Gateway.getInstance().scheduler.runTaskLater(plugin, this, delay)
				isRegistered = true
				return this
			} else {
				throw IllegalStateException("GatewayRunnable already scheduled")
			}
		}
	}

	fun runTaskAsync(plugin: GatewayPlugin): GatewayRunnable {
		synchronized(this) {
			if (!isRegistered) {
				_taskId = Gateway.getInstance().scheduler.runTaskAsync(plugin, this)
				isRegistered = true
				return this
			} else {
				throw IllegalStateException("GatewayRunnable already scheduled")
			}
		}
	}

	fun runTaskLaterAsync(plugin: GatewayPlugin, delay: Long): GatewayRunnable {
		synchronized(this) {
			if (!isRegistered) {
				_taskId = Gateway.getInstance().scheduler.runTaskLaterAsync(plugin, this, delay)
				isRegistered = true
				return this
			} else {
				throw IllegalStateException("GatewayRunnable already scheduled")
			}
		}
	}

	fun runTaskTimer(plugin: GatewayPlugin, delay: Long, period: Long): GatewayRunnable {
		synchronized(this) {
			if (!isRegistered) {
				_taskId = Gateway.getInstance().scheduler.runTaskTimer(plugin, this, delay, period)
				isRegistered = true
				return this
			} else {
				throw IllegalStateException("GatewayRunnable already scheduled")
			}
		}
	}

	fun runTaskTimerAsync(plugin: GatewayPlugin, delay: Long, period: Long): GatewayRunnable {
		synchronized(this) {
			if (!isRegistered) {
				_taskId = Gateway.getInstance().scheduler.runTaskTimerAsync(plugin, this, delay, period)
				isRegistered = true
				return this
			} else {
				throw IllegalStateException("GatewayRunnable already scheduled")
			}
		}
	}

}



