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
class EsuBootstrap @Inject constructor(
    val server: ProxyServer,
    val logger: Logger,
    @DataDirectory val dataDirectory: Path,
    @Named("esu") val container: PluginContainer,
    val metricsFactory: Metrics.Factory
) {

    val esu by lazy {
        EsuPluginVelocity(this)
    }

    @Subscribe
    fun onProxyInitialization(e: ProxyInitializeEvent) {
        // Need this Bootstrap plugin, as velocity scan all methods, including commandManager getter
        // whose dependency is loaded later
        server.eventManager.register(this, esu)
        esu.onProxyInitialization()
    }
}