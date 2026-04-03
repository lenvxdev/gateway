package dev.lenvx.gateway.events

import dev.lenvx.gateway.plugins.GatewayPlugin
import java.util.concurrent.ConcurrentHashMap

class EventsManager {

    private val listeners: MutableList<ListenerPair> = ArrayList()
    private val cachedListeners: MutableMap<Listener, RegisteredCachedListener> = ConcurrentHashMap()

    fun <T : Event> callEvent(event: T): T {
        for (priority in EventPriority.getPrioritiesInOrder()) {
            for ((key, value) in cachedListeners) {
                for (method in value.getListeners(event.javaClass, priority)) {
                    try {
                        method.invoke(key, event)
                    } catch (e: Exception) {
                        System.err.println("Error while passing ${event.javaClass.canonicalName} to the plugin \"${value.plugin.name}\"")
                        e.printStackTrace()
                    }
                }
            }
        }
        return event
    }

    fun registerEvents(plugin: GatewayPlugin, listener: Listener) {
        listeners.add(ListenerPair(plugin, listener))
        cachedListeners[listener] = RegisteredCachedListener(plugin, listener)
    }

    fun unregisterAllListeners(plugin: GatewayPlugin) {
        listeners.removeIf { pair ->
            if (pair.plugin == plugin) {
                cachedListeners.remove(pair.listener)
                true
            } else {
                false
            }
        }
    }

    protected data class ListenerPair(val plugin: GatewayPlugin, val listener: Listener)

}


