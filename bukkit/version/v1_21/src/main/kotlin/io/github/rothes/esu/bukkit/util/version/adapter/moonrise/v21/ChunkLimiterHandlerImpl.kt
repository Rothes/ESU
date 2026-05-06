package io.github.rothes.esu.bukkit.util.version.adapter.moonrise.v21

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import org.bukkit.entity.Player

object ChunkLimiterHandlerImpl : ChunkLimiterHandler() {

    override fun getAllocationTaken(player: Player, type: Type): Long {
        return getMaxRate(type).toLong() - previewAllocation(player, type)
    }

    override fun previewAllocation(player: Player, type: Type): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as AllocatingRateLimiter
        val maxRate = getMaxRate(type)
        return limiter.previewAllocation(System.nanoTime(), maxRate, maxRate.toLong())
    }

    override fun takeAllocation(player: Player, type: Type, take: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as AllocatingRateLimiter
        return limiter.takeAllocation(System.nanoTime(), take.toDouble(), Long.MAX_VALUE)
    }

}