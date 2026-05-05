package io.github.rothes.esu.bukkit.module.essentialcommands.v26_1

import io.github.rothes.esu.bukkit.module.essentialcommands.PlayerChunkTickets
import io.github.rothes.esu.core.util.ReflectionUtils.handle

object AllocatingRateLimiterHandlerImpl : PlayerChunkTickets.AllocatingRateLimiterHandler {

    private val PREVIEW_ALLOCATION = let {
        val clazz = Class.forName("ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter")
        clazz.declaredMethods.first { it.name == "previewAllocation" }.handle
    }

    override fun previewAllocation(
        rateLimiter: Any, type: PlayerChunkTickets.AllocatingRateLimiterHandler.Type
    ): Long {
        return PREVIEW_ALLOCATION.invokeExact(rateLimiter, getMaxRate(type).toLong()) as Long
    }

}