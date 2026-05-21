/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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