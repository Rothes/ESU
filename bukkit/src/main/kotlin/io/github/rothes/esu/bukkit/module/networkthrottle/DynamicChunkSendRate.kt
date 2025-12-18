package io.github.rothes.esu.bukkit.module.networkthrottle

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader
import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.module.configuration.EnableTogglable
import io.papermc.paper.configuration.GlobalConfiguration
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftPlayer
import kotlin.math.max

object DynamicChunkSendRate: CommonFeature<DynamicChunkSendRate.FeatureConfig, EmptyConfiguration>() {

    private const val CHANNEL_ID = "esu:dynamic_chunk_send_rate_limit"

    private val limiterLoad by lazy {
        RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java.getDeclaredField("chunkLoadTicketLimiter")
            .also { it.isAccessible = true }
    }
    private val limiterSend by lazy {
        RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java.getDeclaredField("chunkSendLimiter")
            .also { it.isAccessible = true }
    }
    private val allocation by lazy {
        AllocatingRateLimiter::class.java.getDeclaredField("allocation").also { it.isAccessible = true }
    }
    private val takeCarry by lazy {
        AllocatingRateLimiter::class.java.getDeclaredField("takeCarry").also { it.isAccessible = true }
    }

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (!ServerCompatibility.isProxyMode) {
                core.err("[DynamicChunkSendRate] This server is not on BungeeCord mode or Velocity mode. You should not enable this feature.")
                return Feature.AvailableCheck.fail { "This server is not on BungeeCord mode or Velocity mode".message }
            }

            try {
                limiterLoad
                limiterSend
                allocation
                takeCarry
            } catch (t: Throwable) {
                core.err("[DynamicChunkSendRate] Server not supported: $t")
                return Feature.AvailableCheck.fail { "Server not supported".message }
            }
            null
        }
    }

    override fun onEnable() {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_ID) { channel, player, message ->
            val chunkLoaderData = (player as CraftPlayer).handle.`moonrise$getChunkLoader`()

            val max =
                max(
                    GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkSendRate,
                    GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkLoadRate
                ).let { if (it !in (0.0 .. 10000.0)) 10000.0 else it }
            val limiters = listOf(
                limiterLoad[chunkLoaderData] as AllocatingRateLimiter,
                limiterSend[chunkLoaderData] as AllocatingRateLimiter,
            )
            var times = 20
            fun func() {
                if (--times > 0) {
                    for (rateLimiter in limiters) {
                        takeCarry[rateLimiter] = 24 - max
                        Scheduler.schedule(player, 1) {
                            func()
                        }
                    }
                }
            }
            func()
        }
    }

    override fun onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_ID)
    }

    @Comment("Enable DynamicChunkSendRate. Make sure you have velocity mode on, and installed ESU on velocity.")
    data class FeatureConfig(
        override val enabled: Boolean = ServerCompatibility.isProxyMode,
    ): EnableTogglable, ConfigurationPart

}