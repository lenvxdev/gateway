package dev.lenvx.gateway.scheduler

import dev.lenvx.gateway.Gateway
import java.io.IOException
import java.util.*
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

class Tick(instance: Gateway) {

	private var tickingInterval: Int = 0
	private val tick = AtomicLong(0)
	private var lastMetricsReportMillis: Long = System.currentTimeMillis()
	private val threads: MutableList<Thread> = ArrayList()
	private val asyncTasksQueue: BlockingQueue<GatewayScheduler.GatewaySchedulerTask> = LinkedBlockingQueue()

	init {
		thread(name = "Gateway-Tick-Thread", isDaemon = false) {
			tickingInterval = (1000.0 / Gateway.getInstance().serverProperties.definedTicksPerSecond).toInt()

			for (i in 0 until 4) {
				val t = thread(name = "Gateway-Async-Task-Thread-$i", isDaemon = false) {
					while (instance.isRunning()) {
						try {
							val task = asyncTasksQueue.take()
                        val gatewayTask = task.task
                        try {
                            gatewayTask.run()
							} catch (e: Throwable) {
								System.err.println("Task ${task.taskId} threw an exception: ${e.localizedMessage}")
								e.printStackTrace()
							}
						} catch (e: InterruptedException) {
							Thread.currentThread().interrupt()
							break
						}
					}
				}
				threads.add(t)
			}

			while (instance.isRunning()) {
				val start = System.currentTimeMillis()
				tick.incrementAndGet()
				instance.players.forEach { each ->
					if (each.clientConnection.isReady) {
						try {
							each.playerInteractManager.update()
						} catch (e: IOException) {
							e.printStackTrace()
						}
					}
				}
				instance.worlds.forEach { each ->
					try {
						each.update()
					} catch (e: Exception) {
						when (e) {
							is IllegalArgumentException, is IllegalAccessException -> e.printStackTrace()
							else -> throw e
						}
					}
				}

				val tasks = instance.scheduler.collectTasks(currentTick)
				if (tasks != null) {
					asyncTasksQueue.addAll(tasks.asyncTasks)
					tasks.syncedTasks.forEach { task ->
                    val gatewayTask = task.task
                    try {
                        gatewayTask.run()
						} catch (e: Throwable) {
							System.err.println("Task ${task.taskId} threw an exception: ${e.localizedMessage}")
							e.printStackTrace()
						}
					}
				}

				val end = System.currentTimeMillis()
				if (end - lastMetricsReportMillis >= 60000) {
					lastMetricsReportMillis = end
					val snapshot = instance.runtimeMetrics.snapshotAndReset()
					instance.console.sendMessage(
						"[METRICS] players=${instance.players.size} tick=$currentTick inboundPackets=${snapshot.inboundPackets} outboundPackets=${snapshot.outboundPackets} securityRejects=${snapshot.securityRejects} disconnects=${snapshot.disconnects} asyncQueue=${asyncTasksQueue.size}"
					)
				}
				val sleepTime = tickingInterval.toLong() - (end - start)
				if (sleepTime > 0) {
					try {
						TimeUnit.MILLISECONDS.sleep(sleepTime)
					} catch (e: InterruptedException) {
					}
				}
			}
		}
	}

	val currentTick: Long
		get() = tick.get()

	fun waitAndKillThreads(waitTime: Long) {
		val end = System.currentTimeMillis() + waitTime
		for (t in threads) {
			try {
				t.join(Math.max(end - System.currentTimeMillis(), 1))
			} catch (e: InterruptedException) {
				e.printStackTrace()
			}
		}
	}
}



