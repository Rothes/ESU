package io.github.rothes.esu.bukkit.util.version.adapter.moonrise.v26_1

import ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import io.github.rothes.esu.core.util.UnsafeUtils.usDoubleAccessor
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import org.bukkit.entity.Player

object ChunkLimiterHandlerImpl: ChunkLimiterHandler() {

    private val PREVIEW_ALLOCATION =
        StaggeredRateLimiter::class.java.declaredMethods.first { it.name == "previewAllocation" }.handle
    private val LIMITERS =
        StaggeredRateLimiter::class.java.getDeclaredField("limiters").usObjAccessor
    private val LIMITER =
        Class.forName($$"ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter$Limiter")
            .getDeclaredField("limiter").usObjAccessor
    private val LIMITER_PREVIEW_ALLOCATION =
        Class.forName("ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter")
            .declaredMethods.first { it.name == "previewAllocation" }.handle
    private val LIMITER_MAX_ALLOCATION =
        Class.forName("ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter")
            .getDeclaredField("maxAllocation").usDoubleAccessor

    override fun getAllocationTaken(player: Player, type: Type): Long {
        val rateLimiter = player.moonriseChunkLoader.getLimiter(type) as StaggeredRateLimiter
        val limiters = LIMITERS[rateLimiter] as Array<*> // ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter.Limiter[]

        val limiterHolder = limiters[limiters.size - 2] // Get the 1-second interval one
        val limiter = LIMITER[limiterHolder] // ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter

        val max = LIMITER_MAX_ALLOCATION[limiter].toLong()
        val preview = LIMITER_PREVIEW_ALLOCATION.invoke(limiter, max) as Long

        return max - preview
    }

    override fun previewAllocation(player: Player, type: Type): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as StaggeredRateLimiter
        return PREVIEW_ALLOCATION.invokeExact(limiter, getMaxRate(type).toLong()) as Long
    }

    override fun takeAllocation(player: Player, type: Type, take: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as StaggeredRateLimiter
        return limiter.takeAllocation(take)
    }

}