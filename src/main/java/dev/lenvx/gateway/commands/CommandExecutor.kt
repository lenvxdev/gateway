package dev.lenvx.gateway.commands

interface CommandExecutor {
    fun execute(sender: CommandSender, args: Array<String>)
}
