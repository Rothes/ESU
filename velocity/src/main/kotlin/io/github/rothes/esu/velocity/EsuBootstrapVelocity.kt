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

package io.github.rothes.esu.velocity

import com.google.inject.Inject
import com.google.inject.name.Named
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.util.artifact.AetherLoader
import io.github.rothes.esu.core.util.artifact.MavenResolver
import io.github.rothes.esu.core.util.artifact.relocator.CachedRelocator
import io.github.rothes.esu.core.util.artifact.relocator.PackageRelocator
import io.github.rothes.esu.data.BuildInfo
import io.github.rothes.esu.data.DependencyVersion
import org.bstats.velocity.Metrics
import org.eclipse.aether.artifact.Artifact
import org.slf4j.Logger
import java.io.File
import java.nio.file.Path

@Plugin(
    id = "esu",
    name = "ESU",
    version = BuildInfo.VERSION_NAME,
    authors = ["Rothes"],
    url = "https://github.com/Rothes/ESU",
    dependencies = [
        // Let those plugins load first, so we can restore our ServerChannelInitializerHolder
        Dependency("packetevents", true),
        Dependency("sonar", true),
        Dependency("viaversion", true),
    ]
)
class EsuBootstrapVelocity @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    @param:DataDirectory val dataDirectory: Path,
    @param:Named("esu") val container: PluginContainer,
    val metricsFactory: Metrics.Factory
): EsuBootstrap {

    init {
        EsuBootstrap.setInstance(this)
        AetherLoader.loadAether()
        MavenResolver.loadKotlin()
        loadDependencies()
    }

    val esu = EsuPluginVelocity(this)

    @Subscribe
    fun onProxyInitialization(e: ProxyInitializeEvent) {
        // Need this Bootstrap, as velocity scan all methods,
        // including our dependencies which are loaded later.
        server.eventManager.register(this, esu)
        esu.onProxyInitialization()
    }

    override fun info(message: String) {
        logger.info(message)
    }

    override fun warn(message: String) {
        logger.warn(message)
    }

    override fun err(message: String) {
        logger.error(message)
    }

    override fun err(message: String, throwable: Throwable?) {
        logger.error(message, throwable)
    }

    override val baseConfigPath: Path
        get() {
            return dataDirectory
        }

    private companion object {

        fun loadDependencies() {
            MavenResolver.loadDependencies(
                listOf(
                    "org.ow2.asm:asm-commons:9.8",
                )
            )

            val relocator = PackageRelocator(
                "net/kyori/adventure/" to "adventure/",
                "net/kyori/" to "net/kyori/",

                "org/spongepowered/configurate" to "configurate",

                prefix = "io/github/rothes/esu/lib/"
            )
            val loader = { file: File, artifact: Artifact ->
                if (artifact.extension == "jar"
                    && setOf("net.kyori").contains(artifact.groupId)
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
                    "net.kyori:adventure-nbt:${DependencyVersion.ADVENTURE}",

                    "io.github.rothes:configurate-yaml:4.3.0-b1",
                ),
                loader = loader,
            )

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

                    "org.incendo:cloud-velocity:2.0.0-beta.13",

                    "com.h2database:h2:${DependencyVersion.H2DATABASE}",
                    "com.mysql:mysql-connector-j:8.4.0",
                    "org.mariadb.jdbc:mariadb-java-client:${DependencyVersion.MARIADB_CLIENT}",

                    "it.unimi.dsi:fastutil:8.5.15",
                )
            )
        }

    }

}