package io.github.rothes.esu.bukkit.util.version.remapper

import io.github.rothes.esu.core.util.DataSerializer.deserialize
import io.github.rothes.esu.core.util.DataSerializer.serialize
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

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

    fun verify(file: File, modifier: String? = null, expected: String = file.sha1): Boolean {
        val suffix = modifier?.let { ":$it" } ?: ""
        if (!file.exists())
            return false
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
        @OptIn(ExperimentalStdlibApi::class)
        val File.sha1: String
            get() {
                val messageDigest = MessageDigest.getInstance("SHA-1")
                FileInputStream(this).use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        messageDigest.update(buffer, 0, bytesRead)
                    }
                }
                val hashBytes = messageDigest.digest()
                return hashBytes.toHexString()
            }
    }

}