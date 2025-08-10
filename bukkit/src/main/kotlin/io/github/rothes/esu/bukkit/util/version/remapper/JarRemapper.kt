package io.github.rothes.esu.bukkit.util.version.remapper

import io.github.rothes.esu.bukkit.plugin
import net.neoforged.art.api.Renamer
import net.neoforged.art.api.SignatureStripperConfig
import net.neoforged.art.api.Transformer
import java.io.File
import kotlin.text.startsWith

object JarRemapper {

    private val cacheFolder = plugin.dataFolder.resolve(".cache/remapped")
    private val cached = FileHashes(cacheFolder)

    fun reobf(file: File): File {
        val output = cacheFolder.resolve(file.name)

        if (output.exists()
            && cached.verify(output)
            && cached.verify(file, "mojmap")
            && cached.verify(output, "mappings", MappingsLoader.mappingsHash()))
            return output

        output.parentFile.mkdirs()

        val renamer = Renamer.builder().apply {
            MappingsLoader.loadedFiles.mappings.values.forEach {
                add(Transformer.renamerFactory(it, false))
            }
            add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL))
            lib(File(plugin.javaClass.protectionDomain.codeSource.location.path))
            MappingsLoader.loadedFiles.servers.values.forEach {
                lib(it)
            }
            threads(1)
            logger {
                if (it == "Adding Libraries to Inheritance"
                    || it == "Adding input to inheritance map"
                    || it == "Adding extras"
                    || it == "Sorting"
                    || it.startsWith("Conflicting propagated mapping"))
                    return@logger
                plugin.info("[Remapper] $it")
            }
        }.build()
        renamer.run(file, output)
        cached.store(output)
        cached.store(file, "mojmap")
        cached.store(output, "mappings", MappingsLoader.mappingsHash())
        cached.save()
        return output
    }

}