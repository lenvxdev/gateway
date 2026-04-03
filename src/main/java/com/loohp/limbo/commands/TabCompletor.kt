package com.loohp.limbo.commands

interface TabCompletor {
    fun tabComplete(sender: CommandSender, args: Array<String>): List<String>
}
