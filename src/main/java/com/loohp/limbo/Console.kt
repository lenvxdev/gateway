package com.loohp.limbo

import com.loohp.limbo.commands.CommandSender
import dev.lenvx.gateway.commands.CommandSender as GatewayCommandSender
import java.util.WeakHashMap

class Console private constructor(
    private val delegate: dev.lenvx.gateway.Console
) : CommandSender, GatewayCommandSender by delegate {

    override fun sendMessage(message: String) {
        delegate.sendMessage(message)
    }

    companion object {
        private val wrappers = WeakHashMap<dev.lenvx.gateway.Console, Console>()

        @JvmStatic
        fun wrap(delegate: dev.lenvx.gateway.Console): Console {
            return synchronized(wrappers) {
                wrappers.getOrPut(delegate) { Console(delegate) }
            }
        }
    }
}
