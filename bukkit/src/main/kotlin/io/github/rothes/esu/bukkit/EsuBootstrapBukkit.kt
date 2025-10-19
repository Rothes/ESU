package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.remapper.MappingsLoader
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.artifact.AetherLoader
import io.github.rothes.esu.core.util.artifact.MavenResolver
import io.github.rothes.esu.core.util.artifact.relocator.CachedRelocator
import io.github.rothes.esu.core.util.artifact.relocator.PackageRelocator
import it.unimi.dsi.fastutil.shorts.ShortArrayList
import net.jpountz.lz4.LZ4Factory
import org.bukkit.plugin.java.JavaPlugin
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
        super.onLoad()
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

    override fun baseConfigPath(): Path {
        return dataFolder.toPath()
    }

    private companion object {

        fun loadDependencies() {
            if (!ServerCompatibility.isMojmap) {
                MavenResolver.loadDependencies(
                    listOf(
                        "net.neoforged:AutoRenamingTool:2.0.13",
                    )
                )
                if (ServerCompatibility.hasMojmap)
                    MappingsLoader
            }
            val relocator = PackageRelocator(
                "net/kyori/adventure/" to "io/github/rothes/esu/lib/adventure/",
                "net/kyori/" to "io/github/rothes/esu/lib/net/kyori/",

                "org/bstats" to "io/github/rothes/esu/lib/bstats",
                "de/tr7zw/changeme/nbtapi" to "io/github/rothes/esu/lib/nbtapi",
            )
            MavenResolver.loadDependencies(
                listOf(
                    "net.kyori:adventure-api:${BuildConfig.DEP_VERSION_ADVENTURE}",
                    "net.kyori:adventure-text-minimessage:${BuildConfig.DEP_VERSION_ADVENTURE}",
                    "net.kyori:adventure-text-serializer-ansi:${BuildConfig.DEP_VERSION_ADVENTURE}",
                    "net.kyori:adventure-text-serializer-gson:${BuildConfig.DEP_VERSION_ADVENTURE}",
                    "net.kyori:adventure-text-serializer-legacy:${BuildConfig.DEP_VERSION_ADVENTURE}",
                    "net.kyori:adventure-text-serializer-plain:${BuildConfig.DEP_VERSION_ADVENTURE}",
                    "net.kyori:adventure-platform-bukkit:4.4.1",
                    "org.bstats:bstats-bukkit:3.1.0",
                    "de.tr7zw:item-nbt-api:${BuildConfig.DEP_VERSION_NBTAPI}",
                )
            ) { file, artifact ->
                if (artifact.extension == "jar" && setOf("net.kyori", "org.bstats", "de.tr7zw").contains(artifact.groupId))
                    CachedRelocator.relocate(relocator, file, outputName = "${artifact.groupId}_${artifact.artifactId}")
                else
                    file
            }
            MavenResolver.testDependency("org.lz4:lz4-java:1.8.0") {
                LZ4Factory.fastestInstance()
            }
            MavenResolver.testDependency("it.unimi.dsi:fastutil:8.5.15") {
                // For 1.16.5
                ShortArrayList()
            }
            MavenResolver.loadDependencies(
                listOf(
                    "org.jetbrains.exposed:exposed-core:${BuildConfig.DEP_VERSION_EXPOSED}",
                    "org.jetbrains.exposed:exposed-jdbc:${BuildConfig.DEP_VERSION_EXPOSED}",
                    "org.jetbrains.exposed:exposed-kotlin-datetime:${BuildConfig.DEP_VERSION_EXPOSED}",
                    "org.jetbrains.exposed:exposed-json:${BuildConfig.DEP_VERSION_EXPOSED}",

                    "com.zaxxer:HikariCP:${BuildConfig.DEP_VERSION_HIKARICP}",
                    "org.incendo:cloud-core:2.0.0",
                    "org.incendo:cloud-annotations:2.0.0",
                    "org.incendo:cloud-kotlin-coroutines-annotations:2.0.0",

                    "org.incendo:cloud-paper:2.0.0-beta.10",

                    "com.h2database:h2:${BuildConfig.DEP_VERSION_H2DATABASE}",
                    "org.mariadb.jdbc:mariadb-java-client:${BuildConfig.DEP_VERSION_MARIADB_CLIENT}",

                    "info.debatty:java-string-similarity:2.0.0",
                    "com.hankcs:aho-corasick-double-array-trie:1.2.2",
                )
            )
        }

    }

}