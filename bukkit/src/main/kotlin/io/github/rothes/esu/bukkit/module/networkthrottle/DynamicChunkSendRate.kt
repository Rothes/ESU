package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.plugin
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.module.configuration.EnableTogglable
import io.github.rothes.esu.core.util.extension.math.floorL
import org.bukkit.Bukkit

object DynamicChunkSendRate: CommonFeature<DynamicChunkSendRate.FeatureConfig, EmptyConfiguration>() {

    private const val CHANNEL_ID = "esu:dynamic_chunk_send_rate_limit"


    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (!ServerCompatibility.isProxyMode) return errFail { "This server is not on BungeeCord mode or Velocity mode".message }
            if (!ChunkLimiterHandler.isSupported) return errFail { "Server not supported".message }
            null
        }
    }

    override fun onEnable() {
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, CHANNEL_ID) { channel, player, message ->

            val types = listOf(
                ChunkLimiterHandler.Type.GENERATE,
                ChunkLimiterHandler.Type.LOAD,
            )
            var times = 15
            fun func() {
                if (--times > 0) {
                    val handler = ChunkLimiterHandler.instance
                    for (type in types) {
                        handler.takeAllocation(player, type, handler.getGlobalMaxRate(type).floorL() - 24)
                    }
                    Scheduler.schedule(player, 1) {
                        func()
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