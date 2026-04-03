package dev.lenvx.gateway.consolegui

import dev.lenvx.gateway.Gateway
import java.lang.management.ManagementFactory
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

object SystemInfo {
    @JvmStatic
    fun printInfo() {
        if (!Gateway.noGui) {
            while (true) {
                val runtime = Runtime.getRuntime()
                val format = NumberFormat.getInstance()
                val sb = StringBuilder()
                val maxMemory = runtime.maxMemory()
                val allocatedMemory = runtime.totalMemory()
                val freeMemory = runtime.freeMemory()

                sb.append("Free Memory: ").append(format.format(freeMemory / 1024 / 1024)).append(" MB\n")
                sb.append("Allocated Memory: ").append(format.format(allocatedMemory / 1024 / 1024)).append(" MB\n")
                sb.append("Max Memory: ").append(format.format(maxMemory / 1024 / 1024)).append(" MB\n")
                sb.append("Memory Usage: ").append(format.format((allocatedMemory - freeMemory) / 1024 / 1024)).append("/")
                    .append(format.format(maxMemory / 1024 / 1024)).append(" MB (")
                    .append(((allocatedMemory - freeMemory).toDouble() / maxMemory.toDouble() * 100).toLong().coerceAtLeast(0)).append("%)\n")
                sb.append("\n")

                try {
                    val operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
                    val processLoad = operatingSystemMXBean.processCpuLoad
                    val systemLoad = operatingSystemMXBean.cpuLoad
                    val processors = runtime.availableProcessors()

                    sb.append("Available Processors: ").append(processors).append("\n")
                    sb.append("Process CPU Load: ").append((processLoad * 100).toLong()).append("%\n")
                    sb.append("System CPU Load: ").append((systemLoad * 100).toLong()).append("%\n")
                    GUI.sysText.text = sb.toString()
                } catch (ignore: Exception) {
                }

                try {
                    TimeUnit.MILLISECONDS.sleep(1000)
                } catch (ignored: InterruptedException) {
                }
            }
        }
    }
}

