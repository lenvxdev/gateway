package dev.lenvx.gateway.file

import dev.lenvx.gateway.utils.YamlOrder
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.representer.Representer
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.io.Reader
import java.nio.charset.StandardCharsets

class FileConfiguration {

    private var mapping: MutableMap<String, Any?> = LinkedHashMap()
    private var header: String? = null

    @Throws(IOException::class)
    constructor(file: File) {
        if (file.exists()) {
            FileInputStream(file).use { fis ->
                InputStreamReader(fis, StandardCharsets.UTF_8).use { reader ->
                    reloadConfig(reader)
                }
            }
        } else {
            mapping = LinkedHashMap()
        }
    }

    constructor(input: InputStream) {
        reloadConfig(InputStreamReader(input, StandardCharsets.UTF_8))
    }

    constructor(reader: Reader) {
        reloadConfig(reader)
    }

    @Throws(FileNotFoundException::class)
    fun reloadConfig(file: File): FileConfiguration {
        return reloadConfig(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))
    }

    fun reloadConfig(input: InputStream): FileConfiguration {
        return reloadConfig(InputStreamReader(input, StandardCharsets.UTF_8))
    }

    fun reloadConfig(reader: Reader): FileConfiguration {
        val yml = Yaml()
        mapping = yml.load(reader) ?: LinkedHashMap()
        return this
    }

    fun setHeader(header: String?) {
        this.header = header
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: String, returnType: Class<T>): T? {
        return try {
            val tree = key.split(".").toTypedArray()
            var map = mapping
            for (i in 0 until tree.size - 1) {
                map = map[tree[i]] as MutableMap<String, Any?>
            }
            val lastKey = tree[tree.size - 1]
            val value = map[lastKey]
            if (returnType == String::class.java) {
                value?.toString() as T?
            } else {
                returnType.cast(value)
            }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> set(key: String, value: T?) {
        val tree = key.split(".").toTypedArray()
        var map = mapping
        for (i in 0 until tree.size - 1) {
            var map1 = map[tree[i]] as MutableMap<String, Any?>?
            if (map1 == null) {
                map1 = LinkedHashMap()
                map[tree[i]] = map1
            }
            map = map1
        }
        val lastKey = tree[tree.size - 1]
        if (value != null) {
            map[lastKey] = value
        } else {
            map.remove(lastKey)
        }
    }

    @Throws(IOException::class)
    fun saveToString(): String {
        val options = DumperOptions().apply {
            indent = 2
            isPrettyFlow = true
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        }
        val customRepresenter = Representer(options)
        val customProperty = YamlOrder()
        customRepresenter.setPropertyUtils(customProperty)
        val yaml = Yaml(customRepresenter, options)

        val out = ByteArrayOutputStream()
        PrintWriter(OutputStreamWriter(out, StandardCharsets.UTF_8)).use { pw ->
            header?.let {
                pw.println("#" + it.replace("\n", "\n#"))
            }
            yaml.dump(mapping, pw)
            pw.flush()
        }

        return String(out.toByteArray(), StandardCharsets.UTF_8)
    }

    @Throws(IOException::class)
    fun saveConfig(file: File) {
        val options = DumperOptions().apply {
            indent = 2
            isPrettyFlow = true
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        }
        val customRepresenter = Representer(options)
        val customProperty = YamlOrder()
        customRepresenter.setPropertyUtils(customProperty)
        val yaml = Yaml(customRepresenter, options)

        file.parentFile?.mkdirs()

        PrintWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)).use { pw ->
            header?.let {
                pw.println("#" + it.replace("\n", "\n#"))
            }
            yaml.dump(mapping, pw)
            pw.flush()
        }
    }
}

