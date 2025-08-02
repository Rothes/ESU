package io.github.rothes.esu.bukkit.module.networkthrottle

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.papermc.paper.configuration.GlobalConfiguration
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftPlayer
import kotlin.math.max

object DynamicChunkSendRate {

    private const val CHANNEL_ID = "esu:dynamic_chunk_send_rate_limit"

    private var running = false

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

    fun enable() {
        if (!config.dynamicChunkSendRate.enabled || running)
            return
        if (!ServerCompatibility.proxyMode)
            return plugin.err("[DynamicChunkSendRate] This server is not enabled BungeeCord mode or Velocity mode. You should not enable this module.")
        try {
            limiterLoad
            limiterSend
            allocation
            takeCarry
        } catch (t: Throwable) {
            plugin.err("[DynamicChunkSendRate] Server not supported: $t")
        }

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
        running = true
    }

    fun disable() {
        if (running) {
            Bukkit.getMessenger().unregisterIncomingPluginChannel(plugin, CHANNEL_ID)
            running = false
        }
    }
}