package io.github.rothes.esu.bukkit.util.version.remapper

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.core.util.artifact.PackageRelocator
import java.io.File

object CachedRelocator {

    private val cacheFolder = plugin.dataFolder.resolve(".cache/relocated")
    private val cached = FileHashes(cacheFolder)

    fun relocate(relocator: PackageRelocator, file: File): File {
        val output = cacheFolder.resolve(file.name)

        if (output.exists()
            && cached.verify(output)
            && cached.verify(file, "original"))
            return output

        output.parentFile.mkdirs()
        relocator.relocate(file, output)

        cached.store(output)
        cached.store(file, "original")
        cached.save()
        return output
    }

}