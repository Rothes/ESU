package io.github.rothes.esu.core.util.artifact.relocator

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.artifact.local.FileHashes
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
            && cached.verify(file, "original")
            && (version == null || cached.verify(output, "ver", version)))
            return output

        output.parentFile.mkdirs()
        relocator.relocate(file, output)

        cached.store(output)
        cached.store(file, "original")
        if (version != null)
            cached.store(output, "ver", version)
        cached.save()
        return output
    }

}