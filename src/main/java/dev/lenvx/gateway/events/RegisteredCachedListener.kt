package dev.lenvx.gateway.events

import dev.lenvx.gateway.plugins.GatewayPlugin
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class RegisteredCachedListener(val plugin: GatewayPlugin, listener: Listener) {

    private val listeners: MutableMap<Class<out Event>, MutableMap<EventPriority, MutableList<Method>>> = ConcurrentHashMap()

    init {
        for (method in listener.javaClass.methods) {
            if (method.isAnnotationPresent(EventHandler::class.java) && method.parameterCount == 1 && Event::class.java.isAssignableFrom(method.parameterTypes[0])) {
                @Suppress("UNCHECKED_CAST")
                val eventClass = method.parameterTypes[0] as Class<out Event>
                val mapping = listeners.getOrPut(eventClass) { ConcurrentHashMap() }
                val priority = method.getAnnotation(EventHandler::class.java).priority
                val list = mapping.getOrPut(priority) { ArrayList() }
                list.add(method)
            }
        }
    }

    fun getListeners(eventClass: Class<out Event>, priority: EventPriority): List<Method> {
        val mapping = listeners[eventClass] ?: return emptyList()
        val list = mapping[priority] ?: return emptyList()
        return list.toList()
    }

}


