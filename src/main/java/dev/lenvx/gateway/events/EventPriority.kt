package dev.lenvx.gateway.events

enum class EventPriority(val order: Int) {

    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4),
    MONITOR(5);

    companion object {
        @JvmStatic
        fun getByOrder(order: Int): EventPriority? {
            return entries.find { it.order == order }
        }

        @JvmStatic
        fun getPrioritiesInOrder(): Array<EventPriority> {
            return entries.sortedBy { it.order }.toTypedArray()
        }
    }

}

