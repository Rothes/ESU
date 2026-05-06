package io.github.rothes.esu.bukkit.util.version.adapter.moonrise.v21

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import org.bukkit.entity.Player

object ChunkLimiterHandlerImpl : ChunkLimiterHandler() {

    override fun getAllocationTaken(player: Player, type: Type): Long {
        return getGlobalMaxRate(type).toLong() - previewAllocation(player, type, Long.MAX_VALUE)
    }

    override fun previewAllocation(player: Player, type: Type, take: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as AllocatingRateLimiter
        return limiter.previewAllocation(System.nanoTime(), getGlobalMaxRate(type), take)
    }

    override fun takeAllocation(player: Player, type: Type, take: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as AllocatingRateLimiter
        return limiter.takeAllocation(System.nanoTime(), take.toDouble(), Long.MAX_VALUE)
    }

}