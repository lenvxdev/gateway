package dev.lenvx.gateway.commands

interface TabCompletor {
    fun tabComplete(sender: CommandSender, args: Array<String>): List<String>
}
