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

package io.github.rothes.esu.bukkit.util.version.remapper

import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.version.remapper.transformer.KotlinMetaTransformer
import io.github.rothes.esu.bukkit.util.version.remapper.transformer.ReflectionTransformer
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.artifact.local.FileHashes
import io.github.rothes.esu.core.util.artifact.relocator.PackageRelocator
import io.github.rothes.esu.core.util.extension.ClassUtils.jarFilePath
import io.github.rothes.esu.core.util.version.Version
import net.neoforged.art.api.Renamer
import net.neoforged.art.api.SignatureStripperConfig
import net.neoforged.art.api.Transformer
import net.neoforged.srgutils.IMappingFile
import org.objectweb.asm.tree.MethodNode
import java.io.File

object JarRemapper {

    private const val REMAPPER_VERSION = "6"

    private val cacheFolder = EsuBootstrap.instance.baseConfigPath.resolve(".cache/remapped").toFile()
    private val cached = FileHashes(cacheFolder)

    private val processors = listOf<VersionProcessor>(
        ClassRenamedProcessor(
            Version.fromString("26.2"),
            "net/minecraft/world/entity/EntityType",
            "net/minecraft/world/entity/EntityTypes"
        )
    )

    private val shouldDeobf
        get() = !ServerInfo.isMojmap && ServerInfo.hasMojmap

    fun handle(file: File): File {
        val output = cacheFolder.resolve(file.name)

        if (output.exists()
            && cached.verify(output)
            && cached.verify(file, "original")
            && cached.verify(output, "remapper", REMAPPER_VERSION)
            && cached.verify(output, "mcVersion", ServerInfo.mcVersion.shortString())
            && (!shouldDeobf || cached.verify(output, "mappings", MappingsLoader.mappingsHash())))
            return output

        file.copyTo(output, true)

        for (processor in processors) {
            processor.handle(output)
        }

        if (shouldDeobf) reobf(file, output)

        cached.store(output)
        cached.store(file, "original")
        cached.store(output, "remapper", REMAPPER_VERSION)
        cached.store(output, "mcVersion", ServerInfo.mcVersion.shortString())
        if (shouldDeobf) cached.store(output, "mappings", MappingsLoader.mappingsHash())
        cached.save()
        return output
    }

    fun reobf(input: File, output: File) {
        output.parentFile.mkdirs()

        fun remap(input: File, mappings: List<IMappingFile?>, libs: List<File?>) {
            val renamer = Renamer.builder().apply {
                mappings.forEach { mapping ->
                    if (mapping != null) {
                        add { KotlinMetaTransformer(mapping) }
                        add { ReflectionTransformer(mapping) }
                        add(Transformer.renamerFactory(mapping, false))
                    }
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
            MethodNode().instructions
            renamer.run(input, output)
        }
        remap(input, listOf(MappingsLoader.loadedFiles.mappings.mojmap), listOf(MappingsLoader.loadedFiles.servers.serverMojmap))
        MappingsLoader.craftBukkitPackage?.let { cb ->
            PackageRelocator(
                mapOf(
                    "org/bukkit/craftbukkit/" to "org/bukkit/craftbukkit/$cb/",
                )
            ).relocate(output, output)
        }
        // Do it twice, because some classes(MinecraftServer) is not obf, lib can be conflicting
        remap(output, listOf(MappingsLoader.loadedFiles.mappings.cbCl, MappingsLoader.loadedFiles.mappings.cbMembers), listOf(MappingsLoader.loadedFiles.servers.server, MappingsLoader.loadedFiles.servers.serverCl))
    }

    private abstract class VersionProcessor {

        abstract fun handle(file: File)

    }

    private class ClassRenamedProcessor(
        private val version: Version,
        private val from: String,
        private val to: String,
    ) : VersionProcessor() {

        override fun handle(file: File) {
            val relocates = if (ServerInfo.mcVersion >= version) mapOf(from to to) else mapOf(to to from)
            PackageRelocator(relocates).relocate(file, file)
        }

    }

}