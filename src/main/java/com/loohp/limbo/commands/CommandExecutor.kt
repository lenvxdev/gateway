package com.loohp.limbo.commands

interface CommandExecutor {
    fun execute(sender: CommandSender, args: Array<String>)
}
