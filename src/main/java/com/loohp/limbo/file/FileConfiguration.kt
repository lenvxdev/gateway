package com.loohp.limbo.file

import java.io.File
import java.io.InputStream
import java.io.Reader

class FileConfiguration internal constructor(
    internal val delegate: dev.lenvx.gateway.file.FileConfiguration
) {

    constructor(file: File) : this(dev.lenvx.gateway.file.FileConfiguration(file))

    constructor(input: InputStream) : this(dev.lenvx.gateway.file.FileConfiguration(input))

    constructor(reader: Reader) : this(dev.lenvx.gateway.file.FileConfiguration(reader))

    fun reloadConfig(file: File): FileConfiguration {
        delegate.reloadConfig(file)
        return this
    }

    fun reloadConfig(input: InputStream): FileConfiguration {
        delegate.reloadConfig(input)
        return this
    }

    fun reloadConfig(reader: Reader): FileConfiguration {
        delegate.reloadConfig(reader)
        return this
    }

    fun setHeader(header: String?) {
        delegate.setHeader(header)
    }

    fun <T : Any> get(key: String, returnType: Class<T>): T? {
        return delegate.get(key, returnType)
    }

    fun <T> set(key: String, value: T?) {
        delegate.set(key, value)
    }

    fun saveToString(): String {
        return delegate.saveToString()
    }

    fun save(file: File) {
        delegate.saveConfig(file)
    }
}
