package io.github.rothes.esu.bukkit.util.version.adapter.moonrise.v26_1

import ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import org.bukkit.entity.Player

object ChunkLimiterHandlerImpl: ChunkLimiterHandler() {

    private val PREVIEW_ALLOCATION = StaggeredRateLimiter::class.java.declaredMethods.first { it.name == "previewAllocation" }.handle
    private val LIMITERS = StaggeredRateLimiter::class.java.getDeclaredField("limiters").usObjAccessor
    private val LIMITER =
        Class.forName($$"ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter$Limiter")
            .getDeclaredField("limiter").usObjAccessor
    private val ALLOCATING_RATE_LIMITER_PREVIEW =
        Class.forName("ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter")
            .declaredMethods.first { it.name == "previewAllocation" }.handle

    override fun previewAllocation(player: Player, type: Type): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as StaggeredRateLimiter
        return PREVIEW_ALLOCATION.invokeExact(limiter, getMaxRate(type).toLong()) as Long
    }

    override fun highestAllocation(player: Player, type: Type): Long {
        val rateLimiter = player.moonriseChunkLoader.getLimiter(type) as StaggeredRateLimiter
        val limiters = LIMITERS[rateLimiter] as Array<*> // ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter.Limiter[]

        val last = limiters.last() // Get the one whose allocFactor is 1.0
        val limiter = LIMITER[last] // ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter

        return ALLOCATING_RATE_LIMITER_PREVIEW.invoke(limiter, getMaxRate(type).toLong()) as Long
    }

    override fun takeAllocation(player: Player, type: Type, take: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as StaggeredRateLimiter
        return limiter.takeAllocation(take)
    }

}