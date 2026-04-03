package com.loohp.limbo.plugins

import com.loohp.limbo.file.FileConfiguration

class PluginInfo(file: FileConfiguration) {

    val name: String = file.get("name", String::class.java) ?: "Unknown"
    val description: String = file.get("description", String::class.java) ?: ""
    val author: String = file.get("author", String::class.java) ?: "Unknown"
    val version: String = file.get("version", String::class.java) ?: "Unknown"
    val main: String = file.get("main", String::class.java) ?: ""

    @get:JvmName("getMainClass")
    val mainClass: String
        get() = main
}
