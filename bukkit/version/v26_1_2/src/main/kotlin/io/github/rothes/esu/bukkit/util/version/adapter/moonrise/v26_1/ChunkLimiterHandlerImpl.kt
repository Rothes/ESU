package io.github.rothes.esu.bukkit.util.version.adapter.moonrise.v26_1

import ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter
import io.github.rothes.esu.bukkit.util.version.adapter.moonrise.ChunkLimiterHandler
import io.github.rothes.esu.core.util.ReflectionUtils.get
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import io.github.rothes.esu.core.util.ReflectionUtils.handle
import org.bukkit.entity.Player
import java.util.concurrent.TimeUnit
import kotlin.math.max

object ChunkLimiterHandlerImpl: ChunkLimiterHandler() {

    private val PREVIEW_ALLOCATION =
        StaggeredRateLimiter::class.java.declaredMethods.first { it.name == "previewAllocation" }.handle
    private val LIMITERS =
        StaggeredRateLimiter::class.java.getDeclaredField("limiters").getter
    private val LIMITER =
        Class.forName($$"ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter$Limiter")
            .getDeclaredField("limiter").getter
    private val LIMITER_PREVIEW_ALLOCATION =
        Class.forName("ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter")
            .declaredMethods.first { it.name == "previewAllocation" }.handle
    private val LIMITER_INTERVAL_NS =
        Class.forName("ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter")
            .getDeclaredField("intervalNS").getter
    private val LIMITER_MAX_ALLOCATION =
        Class.forName("ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter")
            .getDeclaredField("maxAllocation").getter

    override fun getAllocationLastSecond(player: Player, type: Type): Long {
        val rateLimiter = player.moonriseChunkLoader.getLimiter(type) as StaggeredRateLimiter
        val limiters = LIMITERS[rateLimiter] as Array<*> // ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter.Limiter[]

        var max = Long.MIN_VALUE
        for (holder in limiters) {
            val limiter = LIMITER[holder] // ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter

            if (LIMITER_INTERVAL_NS[limiter] == TimeUnit.SECONDS.toNanos(3_000)) {
                // https://github.com/Tuinity/Moonrise/pull/181
                continue
            }

            val maxAllocation = (LIMITER_MAX_ALLOCATION[limiter] as Double).toLong()
            val preview = LIMITER_PREVIEW_ALLOCATION.invoke(limiter, maxAllocation) as Long
            max = max(max, maxAllocation - preview)
        }

        return max
    }

    override fun previewAllocation(player: Player, type: Type, take: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as StaggeredRateLimiter
        return PREVIEW_ALLOCATION.invokeExact(limiter, take) as Long
    }

    override fun takeAllocation(player: Player, type: Type, take: Long): Long {
        val limiter = player.moonriseChunkLoader.getLimiter(type) as StaggeredRateLimiter
        return limiter.takeAllocation(take)
    }

}