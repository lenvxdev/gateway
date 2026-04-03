package dev.lenvx.gateway.scheduler

import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.plugins.GatewayPlugin
import java.util.concurrent.atomic.AtomicInteger

class GatewayScheduler {

	private val idProvider = AtomicInteger(0)
	private val registeredTasks: MutableMap<Long, MutableList<GatewaySchedulerTask>> = HashMap()
	private val tasksById: MutableMap<Int, GatewaySchedulerTask> = HashMap()
	private val cancelledTasks: MutableSet<Int> = HashSet()

	protected fun nextId(): Int {
		return idProvider.getAndUpdate { id -> if (id >= Int.MAX_VALUE) 0 else id + 1 }
	}

	fun cancelTask(taskId: Int) {
		if (tasksById.containsKey(taskId)) {
			cancelledTasks.add(taskId)
		}
	}

	fun cancelTask(plugin: GatewayPlugin) {
		for (task in tasksById.values) {
			if (task.plugin.name == plugin.name) {
				cancelledTasks.add(task.taskId)
			}
		}
	}

	protected fun runTask(taskId: Int, plugin: GatewayPlugin, task: GatewayTask): Int {
		return runTaskLater(taskId, plugin, task, 0)
	}

	fun runTask(plugin: GatewayPlugin, task: GatewayTask): Int {
		return runTaskLater(plugin, task, 0)
	}

	protected fun runTaskLater(taskId: Int, plugin: GatewayPlugin, task: GatewayTask, delay: Long): Int {
		var d = delay
		val st = GatewaySchedulerTask(plugin, task, taskId, GatewaySchedulerTaskType.SYNC, 0)
		if (d <= 0) {
			d = 1
		}
		val tick = Gateway.getInstance().heartBeat.currentTick + d
		tasksById[taskId] = st
		registeredTasks.getOrPut(tick) { ArrayList() }.add(st)
		return taskId
	}

	fun runTaskLater(plugin: GatewayPlugin, task: GatewayTask, delay: Long): Int {
		return runTaskLater(nextId(), plugin, task, delay)
	}

	protected fun runTaskAsync(taskId: Int, plugin: GatewayPlugin, task: GatewayTask): Int {
		return runTaskLaterAsync(taskId, plugin, task, 0)
	}

	fun runTaskAsync(plugin: GatewayPlugin, task: GatewayTask): Int {
		return runTaskLaterAsync(plugin, task, 0)
	}

	protected fun runTaskLaterAsync(taskId: Int, plugin: GatewayPlugin, task: GatewayTask, delay: Long): Int {
		var d = delay
		val st = GatewaySchedulerTask(plugin, task, taskId, GatewaySchedulerTaskType.ASYNC, 0)
		if (d <= 0) {
			d = 1
		}
		val tick = Gateway.getInstance().heartBeat.currentTick + d
		tasksById[taskId] = st
		registeredTasks.getOrPut(tick) { ArrayList() }.add(st)
		return taskId
	}

	fun runTaskLaterAsync(plugin: GatewayPlugin, task: GatewayTask, delay: Long): Int {
		return runTaskLaterAsync(nextId(), plugin, task, delay)
	}

	protected fun runTaskTimer(taskId: Int, plugin: GatewayPlugin, task: GatewayTask, delay: Long, period: Long): Int {
		var d = delay
		var p = period
		val st = GatewaySchedulerTask(plugin, task, taskId, GatewaySchedulerTaskType.TIMER_SYNC, p)
		if (d <= 0) {
			d = 1
		}
		if (p <= 0) {
			p = 1
		}
		val tick = Gateway.getInstance().heartBeat.currentTick + d
		tasksById[taskId] = st
		registeredTasks.getOrPut(tick) { ArrayList() }.add(st)
		return taskId
	}

	fun runTaskTimer(plugin: GatewayPlugin, task: GatewayTask, delay: Long, period: Long): Int {
		return runTaskTimer(nextId(), plugin, task, delay, period)
	}

	protected fun runTaskTimerAsync(taskId: Int, plugin: GatewayPlugin, task: GatewayTask, delay: Long, period: Long): Int {
		var d = delay
		var p = period
		val st = GatewaySchedulerTask(plugin, task, taskId, GatewaySchedulerTaskType.TIMER_ASYNC, p)
		if (d <= 0) {
			d = 1
		}
		if (p <= 0) {
			p = 1
		}
		val tick = Gateway.getInstance().heartBeat.currentTick + d
		tasksById[taskId] = st
		registeredTasks.getOrPut(tick) { ArrayList() }.add(st)
		return taskId
	}

	fun runTaskTimerAsync(plugin: GatewayPlugin, task: GatewayTask, delay: Long, period: Long): Int {
		return runTaskTimerAsync(nextId(), plugin, task, delay, period)
	}

	internal fun collectTasks(currentTick: Long): CurrentSchedulerTask? {
		val tasks = registeredTasks.remove(currentTick) ?: return null

		val asyncTasks = mutableListOf<GatewaySchedulerTask>()
		val syncedTasks = mutableListOf<GatewaySchedulerTask>()

		for (task in tasks) {
			val taskId = task.taskId
			if (cancelledTasks.contains(taskId)) {
				cancelledTasks.remove(taskId)
				continue
			}

			when (task.type) {
				GatewaySchedulerTaskType.ASYNC -> asyncTasks.add(task)
				GatewaySchedulerTaskType.SYNC -> syncedTasks.add(task)
				GatewaySchedulerTaskType.TIMER_ASYNC -> {
					asyncTasks.add(task)
					runTaskTimerAsync(task.taskId, task.plugin, task.task, task.period, task.period)
				}
				GatewaySchedulerTaskType.TIMER_SYNC -> {
					syncedTasks.add(task)
					runTaskTimer(task.taskId, task.plugin, task.task, task.period, task.period)
				}
			}
		}

		return CurrentSchedulerTask(syncedTasks, asyncTasks)
	}

	data class CurrentSchedulerTask(
		val syncedTasks: List<GatewaySchedulerTask>,
		val asyncTasks: List<GatewaySchedulerTask>
	)

	class GatewaySchedulerTask internal constructor(
		val plugin: GatewayPlugin,
		val task: GatewayTask,
		val taskId: Int,
		val type: GatewaySchedulerTaskType,
		val period: Long
	)

	enum class GatewaySchedulerTaskType {
		SYNC,
		ASYNC,
		TIMER_SYNC,
		TIMER_ASYNC
	}

}



