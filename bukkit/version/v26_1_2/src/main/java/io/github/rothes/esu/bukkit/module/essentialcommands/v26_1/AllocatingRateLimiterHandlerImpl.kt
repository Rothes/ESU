package io.github.rothes.esu.bukkit.module.essentialcommands.v26_1

import ca.spottedleaf.moonrise.common.misc.StaggeredRateLimiter
import io.github.rothes.esu.bukkit.module.essentialcommands.PlayerChunkTickets
import io.github.rothes.esu.core.util.ReflectionUtils.handle

object AllocatingRateLimiterHandlerImpl : PlayerChunkTickets.AllocatingRateLimiterHandler {

    private val PREVIEW_ALLOCATION = let {
        StaggeredRateLimiter::class.java.declaredMethods.first { it.name == "previewAllocation" }.handle
    }

    override fun previewAllocation(
        rateLimiter: Any, type: PlayerChunkTickets.AllocatingRateLimiterHandler.Type
    ): Long {
        rateLimiter as StaggeredRateLimiter
        return PREVIEW_ALLOCATION.invokeExact(rateLimiter, getMaxRate(type).toLong()) as Long
    }

}