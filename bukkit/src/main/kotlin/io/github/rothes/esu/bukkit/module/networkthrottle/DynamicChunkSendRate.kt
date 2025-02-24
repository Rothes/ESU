package io.github.rothes.esu.bukkit.module.networkthrottle

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import ca.spottedleaf.moonrise.patches.chunk_system.player.RegionizedPlayerChunkLoader
import com.google.gson.reflect.TypeToken
import io.github.rothes.esu.bukkit.module.NetworkThrottleModule.config
import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.DataSerializer.decode
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.papermc.paper.configuration.GlobalConfiguration
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.entity.CraftPlayer
import java.util.*
import kotlin.math.max

object DynamicChunkSendRate {

    private const val CHANNEL_ID = "esu:dynamic_chunk_send_rate"

    private var running = false

    private val limiter by lazy {
        RegionizedPlayerChunkLoader.PlayerChunkLoaderData::class.java.getDeclaredField("chunkLoadTicketLimiter")
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
            return plugin.err("This server is not enabled BungeeCord mode or Velocity mode, You should not enable NetworkThrottle/DynamicChunkSendRate!")

        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_ID) { channel, player, message ->
            val players = message.decode(object : TypeToken<List<UUID>>() {}).mapNotNull { Bukkit.getPlayer(it) as? CraftPlayer }
            for (player in players) {
                val chunkLoaderData = player.handle.`moonrise$getChunkLoader`()

                val max = max(
                    max(
                        GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkSendRate,
                        GlobalConfiguration.get().chunkLoadingBasic.playerMaxChunkLoadRate
                    ),
                    1000.0
                )
                val limiter = limiter[chunkLoaderData] as AllocatingRateLimiter
                var times = 20
                plugin.info("Limit ${player.name} for 1s")
                fun func() {
                    if (--times > 0) {
                        takeCarry[limiter] = 3 - max
                        Scheduler.schedule(player, 1) {
                            func()
                        }
                    }
                }
                func()
            }
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