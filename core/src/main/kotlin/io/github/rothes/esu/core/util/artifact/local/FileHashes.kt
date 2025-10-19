package io.github.rothes.esu.core.util.artifact.local

import io.github.rothes.esu.core.util.DataSerializer.deserialize
import io.github.rothes.esu.core.util.DataSerializer.serialize
import io.github.rothes.esu.core.util.artifact.HashUtils
import java.io.File

private const val FILE_HASHES_FILENAME = "fileHashes"

class FileHashes(
    folder: File
) {

    val dataFile = folder.resolve(FILE_HASHES_FILENAME)

    private val data: Data = if (!dataFile.exists()) Data() else dataFile.readText().deserialize()

    private operator fun get(key: String): String? = data.sha1[key]
    private operator fun set(key: String, value: String) = data.sha1.set(key, value)

    private operator fun get(file: File): String? = get(file.name)
    private operator fun set(file: File, value: String) = data.sha1.set(file.name, value)

    fun verify(file: File, modifier: String? = null): Boolean {
        if (!file.exists())
            return false
        return verify(file, modifier, file.sha1)
    }

    fun verify(file: File, modifier: String? = null, expected: String): Boolean {
        if (!file.exists())
            return false
        val suffix = modifier?.let { ":$it" } ?: ""
        return expected == get(file.name + suffix)
    }

    fun store(file: File, modifier: String? = null, value: String = file.sha1) {
        val suffix = modifier?.let { ":$it" } ?: ""
        set(file.name + suffix, value)
    }

    fun clear() {
        data.sha1.clear()
    }

    fun save() {
        if (!dataFile.exists()) {
            dataFile.parentFile.mkdirs()
            dataFile.createNewFile()
        }
        dataFile.writer().use { writer ->
            writer.append(data.serialize())
        }
    }

    inner class Data(
        val sha1: MutableMap<String, String> = linkedMapOf()
    )

    companion object {
        val File.sha1: String
            get() = HashUtils.calculateSha1(this)
    }

}