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
import org.bstats.velocity.Metrics
import org.slf4j.Logger
import java.nio.file.Path

@Plugin(
    id = "esu",
    name = "ESU",
    version = BuildConfig.VERSION_NAME,
    authors = ["Rothes"],
    url = "https://github.com/Rothes/ESU",
    dependencies = [
        Dependency("packetevents", true),
        // Let those plugins load first, so we can restore our ServerChannelInitializerHolder
        Dependency("sonar", true),
        Dependency("viaversion", true),
    ]
)
class EsuBootstrapVelocity @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    @DataDirectory val dataDirectory: Path,
    @Named("esu") val container: PluginContainer,
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

    override fun baseConfigPath(): Path {
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
                "net/kyori/adventure/" to "io/github/rothes/esu/lib/adventure/",
                "net/kyori/" to "io/github/rothes/esu/lib/net/kyori/",
                "org/bstats" to "io/github/rothes/esu/lib/bstats",
            )
            MavenResolver.loadDependencies(
                listOf(
                    "net.kyori:adventure-api:${BuildConfig.DEP_ADVENTURE_VERSION}",
                    "net.kyori:adventure-text-minimessage:${BuildConfig.DEP_ADVENTURE_VERSION}",
                    "net.kyori:adventure-text-serializer-ansi:${BuildConfig.DEP_ADVENTURE_VERSION}",
                    "net.kyori:adventure-text-serializer-gson:${BuildConfig.DEP_ADVENTURE_VERSION}",
                    "net.kyori:adventure-text-serializer-legacy:${BuildConfig.DEP_ADVENTURE_VERSION}",
                    "net.kyori:adventure-text-serializer-plain:${BuildConfig.DEP_ADVENTURE_VERSION}",
                )
            ) { file, artifact ->
                if (setOf("net.kyori").contains(artifact.groupId))
                    CachedRelocator.relocate(relocator, file, "3")
                else
                    file
            }

            MavenResolver.loadDependencies(
                listOf(
                    "org.jetbrains.exposed:exposed-core:${BuildConfig.DEP_EXPOSED_VERSION}",
                    "org.jetbrains.exposed:exposed-jdbc:${BuildConfig.DEP_EXPOSED_VERSION}",
                    "org.jetbrains.exposed:exposed-kotlin-datetime:${BuildConfig.DEP_EXPOSED_VERSION}",
                    "org.jetbrains.exposed:exposed-json:${BuildConfig.DEP_EXPOSED_VERSION}",

                    "com.zaxxer:HikariCP:6.3.0",
                    "org.incendo:cloud-core:2.0.0",
                    "org.incendo:cloud-annotations:2.0.0",
                    "org.incendo:cloud-kotlin-coroutines-annotations:2.0.0",

                    "org.incendo:cloud-velocity:2.0.0-beta.10",

                    "com.h2database:h2:2.3.232",
                    "com.mysql:mysql-connector-j:8.4.0",
                    "org.mariadb.jdbc:mariadb-java-client:3.5.3",
                )
            )
        }

    }

}