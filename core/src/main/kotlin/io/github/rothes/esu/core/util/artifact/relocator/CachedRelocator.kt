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

package io.github.rothes.esu.core.util.artifact.relocator

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.artifact.local.FileHashes
import io.github.rothes.esu.core.util.artifact.local.FileHashes.Companion.sha1
import java.io.File

object CachedRelocator {

    private val cacheFolder = EsuBootstrap.instance.baseConfigPath.toFile().resolve(".cache/relocated")
    private val cached = FileHashes(cacheFolder)

    fun relocate(
        relocator: PackageRelocator,
        file: File,
        outputName: String = file.nameWithoutExtension,
        version: String? = null
    ): File {
        val output = cacheFolder.resolve("$outputName.jar")
        if (output.exists()
            && cached.verify(output)
            && cached.verify(output, "original", file.sha1)
            && (version == null || cached.verify(output, "ver", version)))
            return output

        output.parentFile.mkdirs()
        relocator.relocate(file, output)

        cached.store(output)
        cached.store(output, "original", file.sha1)
        if (version != null)
            cached.store(output, "ver", version)
        cached.save()
        return output
    }

}