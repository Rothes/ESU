package io.github.rothes.esu.bukkit.util.version.remapper

import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.artifact.local.FileHashes
import io.github.rothes.esu.core.util.artifact.relocator.PackageRelocator
import io.github.rothes.esu.core.util.extension.ClassUtils.jarFilePath
import net.neoforged.art.api.Renamer
import net.neoforged.art.api.SignatureStripperConfig
import net.neoforged.art.api.Transformer
import net.neoforged.srgutils.IMappingFile
import java.io.File

object JarRemapper {

    private const val REMAPPER_VERSION = "4"

    private val cacheFolder = EsuBootstrap.instance.baseConfigPath().resolve(".cache/remapped").toFile()
    private val cached = FileHashes(cacheFolder)

    fun reobf(file: File): File {
        val output = cacheFolder.resolve(file.name)

        if (output.exists()
            && cached.verify(output)
            && cached.verify(file, "mojmap")
            && cached.verify(output, "remapper", REMAPPER_VERSION)
            && cached.verify(output, "mappings", MappingsLoader.mappingsHash()))
            return output

        output.parentFile.mkdirs()

        fun remap(input: File, mappings: List<IMappingFile?>, libs: List<File?>) {
            val renamer = Renamer.builder().apply {
                mappings.forEach {
                    if (it != null) add(Transformer.renamerFactory(it, false))
                }
                add(Transformer.signatureStripperFactory(SignatureStripperConfig.ALL))
                lib(File(EsuBootstrap::class.java.jarFilePath))
                libs.forEach {
                    if (it != null) lib(it)
                }
                threads(1)
                logger {
                    if (it == "Adding Libraries to Inheritance"
                        || it == "Adding input to inheritance map"
                        || it == "Adding extras"
                        || it == "Sorting"
                        || it.startsWith("Conflicting propagated mapping"))
                        return@logger
                    EsuBootstrap.instance.info("[Remapper] $it")
                }
            }.build()
            renamer.run(input, output)
        }
        remap(file, listOf(MappingsLoader.loadedFiles.mappings.mojmap), listOf(MappingsLoader.loadedFiles.servers.serverMojmap))
        MappingsLoader.craftBukkitPackage?.let { cb ->
            PackageRelocator(
                mapOf(
                    "org/bukkit/craftbukkit/" to "org/bukkit/craftbukkit/$cb/",
                )
            ).relocate(output, output)
        }
        // Do it twice, because some classes(MinecraftServer) is not obf, lib can be conflicting
        remap(output, listOf(MappingsLoader.loadedFiles.mappings.cbCl, MappingsLoader.loadedFiles.mappings.cbMembers), listOf(MappingsLoader.loadedFiles.servers.server, MappingsLoader.loadedFiles.servers.serverCl))
        cached.store(output)
        cached.store(file, "mojmap")
        cached.store(output, "remapper", REMAPPER_VERSION)
        cached.store(output, "mappings", MappingsLoader.mappingsHash())
        cached.save()
        return output
    }

}