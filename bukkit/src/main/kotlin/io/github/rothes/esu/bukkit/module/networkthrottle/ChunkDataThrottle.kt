package io.github.rothes.esu.bukkit.module.networkthrottle

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkDataThrottleHandler
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.extension.checkPacketEvents
import io.github.rothes.esu.bukkit.util.version.versioned
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration
import io.github.rothes.esu.core.module.configuration.EmptyConfiguration
import io.github.rothes.esu.core.user.User
import org.incendo.cloud.annotations.Command

object ChunkDataThrottle: CommonFeature<ChunkDataThrottle.FeatureConfig, EmptyConfiguration>() {

    val handler by lazy { ChunkDataThrottleHandler::class.java.versioned() }

    init {
        if (ServerCompatibility.serverVersion >= 18) registerFeature(handler)
    }

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: checkPacketEvents() ?: let {
            if (ServerCompatibility.serverVersion < 18) {
                core.err("[ChunkDataThrottle] This feature requires Minecraft 1.18+")
                return Feature.AvailableCheck.fail { "This feature requires Minecraft 1.18+".message }
            }
            null
        }
    }

    override fun onEnable() {
        registerCommands(object {
            @Command("esu networkThrottle chunkDataThrottle stats")
            @ShortPerm("chunkDataThrottle")
            fun chunkDataThrottleView(sender: User) {
                val (minimalChunks, resentChunks, resentBlocks) = handler.counter
                sender.message("minimalChunks: $minimalChunks ; resentChunks: $resentChunks ; resentBlocks: $resentBlocks")
            }
        })
    }

    @Comment("""
            Helps to reduce chunk upload bandwidth. Plugin will compress invisible blocks in chunk data packet.
            If necessary, we send a full chunk data again.
            This can save about 50% bandwidth usage in overworld and 30% in nether averagely.
            Make sure you have enabled network-compression on proxy or this server.
            """)
    class FeatureConfig(): BaseFeatureConfiguration(true)

}