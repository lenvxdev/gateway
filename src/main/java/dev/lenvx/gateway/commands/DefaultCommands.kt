package dev.lenvx.gateway.commands

import dev.lenvx.gateway.Console
import dev.lenvx.gateway.Gateway
import dev.lenvx.gateway.player.Player
import dev.lenvx.gateway.utils.GameMode
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.md_5.bungee.api.ChatColor

class DefaultCommands : CommandExecutor, TabCompletor {

    private fun hasCommandPermission(sender: CommandSender, node: String): Boolean {
        return sender.hasPermission("gatewayserver.$node") || sender.hasPermission("limboserver.$node")
    }

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty()) {
            return
        }

        if (args[0].equals("version", ignoreCase = true)) {
            if (hasCommandPermission(sender, "version")) {
                sender.sendMessage("${ChatColor.GRAY}This server is running Gateway version ${Gateway.instance!!.GATEWAY_IMPLEMENTATION_VERSION} (MC: ${Gateway.instance!!.SERVER_IMPLEMENTATION_VERSION})")
            } else {
                sender.sendMessage("${ChatColor.RED}You do not have permission to use that command!")
            }
            return
        }

        if (args[0].equals("spawn", ignoreCase = true)) {
            if (hasCommandPermission(sender, "spawn")) {
                if (args.size == 1 && sender is Player) {
                    sender.teleport(Gateway.instance!!.serverProperties.worldSpawn)
                    sender.sendMessage("${ChatColor.GOLD}Teleporting you to spawn!")
                } else if (args.size == 2) {
                    val player = Gateway.instance!!.getPlayer(args[1])
                    if (player != null) {
                        player.teleport(Gateway.instance!!.serverProperties.worldSpawn)
                        sender.sendMessage("${ChatColor.GOLD}Teleporting ${player.name} to spawn!")
                    } else {
                        sender.sendMessage("${ChatColor.RED}Player not found!")
                    }
                } else {
                    sender.sendMessage("${ChatColor.RED}Invalid command usage!")
                }
            } else {
                sender.sendMessage("${ChatColor.RED}You do not have permission to use that command!")
            }
            return
        }

        if (args[0].equals("stop", ignoreCase = true)) {
            if (hasCommandPermission(sender, "stop")) {
                Gateway.instance!!.stopServer()
            } else {
                sender.sendMessage("${ChatColor.RED}You do not have permission to use that command!")
            }
            return
        }

        if (args[0].equals("kick", ignoreCase = true)) {
            if (hasCommandPermission(sender, "kick")) {
                var reason: Component = Component.translatable("multiplayer.disconnect.kicked")
                var customReason = false
                if (args.size > 1) {
                    val player = Gateway.instance!!.getPlayer(args[1])
                    if (player != null) {
                        val reasonRaw = args.drop(2).joinToString(" ")
                        if (reasonRaw.trim().isNotEmpty()) {
                            reason = LegacyComponentSerializer.legacySection().deserialize(reasonRaw)
                            customReason = true
                        }
                        player.disconnect(reason)
                        if (customReason) {
                            sender.sendMessage("${ChatColor.RED}Kicked the player ${player.name} for the reason: ${LegacyComponentSerializer.legacySection().serialize(reason)}")
                        } else {
                            sender.sendMessage("${ChatColor.RED}Kicked the player ${player.name}")
                        }
                    } else {
                        sender.sendMessage("${ChatColor.RED}Player is not online!")
                    }
                } else {
                    sender.sendMessage("${ChatColor.RED}You have to specify a player!")
                }
            } else {
                sender.sendMessage("${ChatColor.RED}You do not have permission to use that command!")
            }
            return
        }

        if (args[0].equals("gamemode", ignoreCase = true)) {
            if (hasCommandPermission(sender, "gamemode")) {
                if (args.size > 1) {
                    val player = if (args.size > 2) Gateway.instance!!.getPlayer(args[2]) else (sender as? Player)
                    if (player == null) {
                        sender.sendMessage("${ChatColor.RED}You have to specify a player!")
                    } else {
                        try {
                            player.gamemode = GameMode.fromId(args[1].toInt()) ?: throw IllegalArgumentException()
                        } catch (_: Exception) {
                            try {
                                player.gamemode = GameMode.fromName(args[1]) ?: throw IllegalArgumentException()
                            } catch (_: Exception) {
                                sender.sendMessage("${ChatColor.RED}Invalid usage!")
                                return
                            }
                        }
                        sender.sendMessage("${ChatColor.GOLD}Updated gamemode to ${player.gamemode.name}")
                    }
                } else {
                    sender.sendMessage("${ChatColor.RED}Invalid usage!")
                }
            } else {
                sender.sendMessage("${ChatColor.RED}You do not have permission to use that command!")
            }
            return
        }

        if (args[0].equals("say", ignoreCase = true)) {
            if (hasCommandPermission(sender, "say")) {
                if (sender is Console) {
                    if (args.size > 1) {
                        val message = "[Server] ${args.drop(1).joinToString(" ")}"
                        Gateway.instance!!.console.sendMessage(message)
                        for (each in Gateway.instance!!.getPlayers()) {
                            each.sendMessage(message)
                        }
                    }
                } else {
                    if (args.size > 1) {
                        val message = "[${sender.name}] ${args.drop(1).joinToString(" ")}"
                        Gateway.instance!!.console.sendMessage(message)
                        for (each in Gateway.instance!!.getPlayers()) {
                            each.sendMessage(message)
                        }
                    }
                }
            } else {
                sender.sendMessage("${ChatColor.RED}You do not have permission to use that command!")
            }
            return
        }

        if (args[0].equals("whitelist", ignoreCase = true)) {
            if (hasCommandPermission(sender, "whitelist")) {
                if (args.size != 2) {
                    sender.sendMessage("${ChatColor.RED}Invalid usage!")
                } else if (!args[1].equals("reload", ignoreCase = true)) {
                    sender.sendMessage("${ChatColor.RED}Invalid usage!")
                } else {
                    Gateway.instance!!.serverProperties.reloadWhitelist()
                    sender.sendMessage("Whitelist has been reloaded")
                }
            } else {
                sender.sendMessage("${ChatColor.RED}You do not have permission to use that command!")
            }
            return
        }
    }

    override fun tabComplete(sender: CommandSender, args: Array<String>): List<String> {
        val tab = mutableListOf<String>()
        when (args.size) {
            0 -> {
                if (hasCommandPermission(sender, "spawn")) tab.add("spawn")
                if (hasCommandPermission(sender, "kick")) tab.add("kick")
                if (hasCommandPermission(sender, "stop")) tab.add("stop")
                if (hasCommandPermission(sender, "say")) tab.add("say")
                if (hasCommandPermission(sender, "gamemode")) tab.add("gamemode")
            }
            1 -> {
                val arg0Lower = args[0].lowercase()
                if (hasCommandPermission(sender, "spawn") && "spawn".startsWith(arg0Lower)) tab.add("spawn")
                if (hasCommandPermission(sender, "kick") && "kick".startsWith(arg0Lower)) tab.add("kick")
                if (hasCommandPermission(sender, "stop") && "stop".startsWith(arg0Lower)) tab.add("stop")
                if (hasCommandPermission(sender, "say") && "say".startsWith(arg0Lower)) tab.add("say")
                if (hasCommandPermission(sender, "gamemode") && "gamemode".startsWith(arg0Lower)) tab.add("gamemode")
            }
            2 -> {
                if (hasCommandPermission(sender, "kick") && args[0].equals("kick", ignoreCase = true)) {
                    val arg1Lower = args[1].lowercase()
                    for (player in Gateway.instance!!.getPlayers()) {
                        if (player.name.lowercase().startsWith(arg1Lower)) {
                            tab.add(player.name)
                        }
                    }
                }
                if (hasCommandPermission(sender, "gamemode") && args[0].equals("gamemode", ignoreCase = true)) {
                    val arg1Lower = args[1].lowercase()
                    for (mode in GameMode.entries) {
                        if (mode.name.lowercase().startsWith(arg1Lower)) {
                            tab.add(mode.name)
                        }
                    }
                }
            }
            3 -> {
                if (hasCommandPermission(sender, "gamemode") && args[0].equals("gamemode", ignoreCase = true)) {
                    val arg2Lower = args[2].lowercase()
                    for (player in Gateway.instance!!.getPlayers()) {
                        if (player.name.lowercase().startsWith(arg2Lower)) {
                            tab.add(player.name)
                        }
                    }
                }
            }
        }
        return tab
    }
}



