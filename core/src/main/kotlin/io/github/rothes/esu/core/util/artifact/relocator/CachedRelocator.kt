package io.github.rothes.esu.core.util.artifact.relocator

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.artifact.local.FileHashes
import io.github.rothes.esu.core.util.artifact.local.FileHashes.Companion.sha1
import java.io.File

object CachedRelocator {

    private val cacheFolder = EsuBootstrap.instance.baseConfigPath().toFile().resolve(".cache/relocated")
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