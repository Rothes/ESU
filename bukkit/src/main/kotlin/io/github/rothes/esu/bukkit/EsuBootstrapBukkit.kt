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

package io.github.rothes.esu.bukkit

import com.github.luben.zstd.Zstd
import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.version.remapper.MappingsLoader
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.artifact.AetherLoader
import io.github.rothes.esu.core.util.artifact.MavenResolver
import io.github.rothes.esu.core.util.artifact.relocator.CachedRelocator
import io.github.rothes.esu.core.util.artifact.relocator.PackageRelocator
import io.github.rothes.esu.data.DependencyVersion
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import net.jpountz.lz4.LZ4Factory
import org.bukkit.plugin.java.JavaPlugin
import org.eclipse.aether.artifact.Artifact
import java.io.File
import java.nio.file.Path
import java.util.logging.Level

class EsuBootstrapBukkit: JavaPlugin(), EsuBootstrap {

    init {
        EsuBootstrap.setInstance(this)
        AetherLoader.loadAether()
        MavenResolver.loadKotlin()
        loadDependencies()
    }

    val esu = EsuPluginBukkit(this)

    override fun onLoad() {
        esu.onLoad()
    }

    override fun onEnable() {
        esu.onEnable()
    }

    override fun onDisable() {
        esu.onDisable()
    }

    override fun info(message: String) {
        logger.log(Level.INFO, message)
    }

    override fun warn(message: String) {
        logger.log(Level.WARNING, message)
    }

    override fun err(message: String) {
        logger.log(Level.SEVERE, message)
    }

    override fun err(message: String, throwable: Throwable?) {
        logger.log(Level.SEVERE, message, throwable)
    }

    override val baseConfigPath: Path
        get() = dataFolder.toPath()

    private companion object {

        fun loadDependencies() {
            if (!ServerInfo.isMojmap) {
                MavenResolver.loadDependencies(
                    listOf(
                        "net.neoforged:AutoRenamingTool:2.0.13",
                    ),
                    extraRepo = listOf(MavenResolver.MavenRepos.NEO_FORGED)
                )
                if (ServerInfo.hasMojmap)
                    MappingsLoader
            }
            val relocator = PackageRelocator(
                "net/kyori/adventure/" to "adventure/",
                "net/kyori/" to "net/kyori/",

                "org/bstats" to "bstats",
                "de/tr7zw/changeme/nbtapi" to "nbtapi",

                "org/spongepowered/configurate" to "configurate",

                prefix = "io/github/rothes/esu/lib/"
            )
            val loader = { file: File, artifact: Artifact ->
                if (artifact.extension == "jar"
                    && setOf("net.kyori", "org.bstats", "de.tr7zw").contains(artifact.groupId)
                    || artifact.artifactId.startsWith("configurate")
                )
                    CachedRelocator.relocate(relocator, file, outputName = "${artifact.groupId}_${artifact.artifactId}")
                else
                    file
            }
            MavenResolver.loadDependencies(
                listOf(
                    "net.kyori:adventure-api:${DependencyVersion.ADVENTURE}",
                    "net.kyori:adventure-text-minimessage:${DependencyVersion.ADVENTURE}",
                    "net.kyori:adventure-text-serializer-ansi:${DependencyVersion.ADVENTURE}",
                    "net.kyori:adventure-text-serializer-gson:${DependencyVersion.ADVENTURE}",
                    "net.kyori:adventure-text-serializer-legacy:${DependencyVersion.ADVENTURE}",
                    "net.kyori:adventure-text-serializer-plain:${DependencyVersion.ADVENTURE}",
                    "net.kyori:adventure-platform-bukkit:4.4.1",

                    "io.github.rothes:configurate-yaml:4.3.0-b1",
                ),
                loader = loader,
            )
            MavenResolver.loadDependencies(
                listOf(
                    "org.bstats:bstats-bukkit:3.1.0",
                    "de.tr7zw:item-nbt-api:${DependencyVersion.NBTAPI}",
                ),
                extraRepo = listOf(MavenResolver.MavenRepos.CODEMC),
                loader = loader,
            )
            MavenResolver.testDependency("at.yawk.lz4:lz4-java:1.10.4") {
                LZ4Factory.fastestInstance()
            }
            MavenResolver.testDependency("com.github.luben:zstd-jni:1.5.7-7") {
                Zstd.compressBound(16)
            }
            MavenResolver.testDependency("it.unimi.dsi:fastutil:8.5.15") {
                // For 1.16.5
                ShortArrayList()
            }
            MavenResolver.loadDependencies(
                listOf(
                    "org.jetbrains.exposed:exposed-core:${DependencyVersion.EXPOSED}",
                    "org.jetbrains.exposed:exposed-jdbc:${DependencyVersion.EXPOSED}",
                    "org.jetbrains.exposed:exposed-kotlin-datetime:${DependencyVersion.EXPOSED}",
                    "org.jetbrains.exposed:exposed-json:${DependencyVersion.EXPOSED}",

                    "com.zaxxer:HikariCP:${DependencyVersion.HIKARICP}",
                    "org.incendo:cloud-core:2.0.0",
                    "org.incendo:cloud-annotations:2.0.0",
                    "org.incendo:cloud-kotlin-coroutines-annotations:2.0.0",
                    "org.incendo:cloud-kotlin-extensions:2.0.0",

                    "org.incendo:cloud-paper:2.0.0-beta.15",

                    "com.h2database:h2:${DependencyVersion.H2DATABASE}",
                    "org.mariadb.jdbc:mariadb-java-client:${DependencyVersion.MARIADB_CLIENT}",

                    "info.debatty:java-string-similarity:2.0.0",
                    "com.hankcs:aho-corasick-double-array-trie:1.2.2",
                    "io.github.ranlee1:jpinyin:1.0.1",
                )
            )
        }

    }

}