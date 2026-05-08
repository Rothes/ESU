package io.github.rothes.esu.bukkit.util.version.adapter.moonrise.v21

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import org.bukkit.entity.Player

object ChunkLimiterHandlerImpl : ChunkLimiterHandler() {

    private const val MAX_RATE: Long = 10000

    private fun previewAllocation(player: Player, type: Type, take: Long, time: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as AllocatingRateLimiter
        return limiter.previewAllocation(time, getGlobalMaxRate(type), take)
    }

    override fun getAllocationLastSecond(player: Player, type: Type): Long {
        return getGlobalMaxRate(type).toLong() - previewAllocation(player, type, MAX_RATE)
    }

    override fun previewAllocation(player: Player, type: Type, take: Long): Long {
        return previewAllocation(player, type, take, System.nanoTime())
    }

    override fun takeAllocation(player: Player, type: Type, take: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as AllocatingRateLimiter
        return limiter.takeAllocation(System.nanoTime(), take.toDouble(), MAX_RATE)
    }

}