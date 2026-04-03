package dev.lenvx.gateway.events

import java.lang.annotation.Documented

@Documented
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventHandler(
    val priority: EventPriority = EventPriority.NORMAL
)

