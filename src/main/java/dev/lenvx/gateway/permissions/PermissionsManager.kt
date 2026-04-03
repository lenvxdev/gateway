package dev.lenvx.gateway.permissions

import dev.lenvx.gateway.Console
import dev.lenvx.gateway.commands.CommandSender
import dev.lenvx.gateway.file.FileConfiguration
import dev.lenvx.gateway.player.Player
import java.io.File
import java.io.IOException
import java.util.UUID

class PermissionsManager {

    val users: MutableMap<UUID, MutableList<String>> = mutableMapOf()
    val permissions: MutableMap<String, MutableList<String>> = mutableMapOf()

    @Suppress("UNCHECKED_CAST")
    @Throws(IOException::class)
    fun loadDefaultPermissionFile(file: File) {
        val config = FileConfiguration(file)
        permissions["default"] = mutableListOf()
        try {
            val groups = config.get("groups", Map::class.java) as Map<String, List<String>>
            for (key in groups.keys) {
                val nodes = mutableListOf<String>()
                nodes.addAll(groups[key]!!)
                permissions[key] = nodes
            }
        } catch (e: Exception) {
        }
        try {
            val players = config.get("players", Map::class.java) as Map<String, List<String>>
            for (key in players.keys) {
                val groups = mutableListOf<String>()
                groups.addAll(players[key]!!)
                try {
                    val uuid = UUID.fromString(key)
                    users[uuid] = groups
                } catch (e: IllegalArgumentException) {
                    System.err.println("PermissionsManager: Player '$key' is not a valid UUID. Please use UUIDs for security.")
                }
            }
        } catch (e: Exception) {
        }
    }

    fun hasPermission(sender: CommandSender, permission: String): Boolean {
        return when (sender) {
            is Console -> true
            is Player -> {
                val userGroups = users[sender.uniqueId]
                if (userGroups != null && userGroups.any { group ->
                        permissions[group]?.any { it.equals(permission, ignoreCase = true) } == true
                    }) {
                    true
                } else {
                    permissions["default"]?.any { it.equals(permission, ignoreCase = true) } == true
                }
            }
            else -> false
        }
    }
}

