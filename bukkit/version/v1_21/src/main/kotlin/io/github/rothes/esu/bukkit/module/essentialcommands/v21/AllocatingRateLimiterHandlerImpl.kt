package io.github.rothes.esu.bukkit.module.essentialcommands.v21

import ca.spottedleaf.moonrise.common.misc.AllocatingRateLimiter
import io.github.rothes.esu.bukkit.module.essentialcommands.PlayerChunkTickets

object AllocatingRateLimiterHandlerImpl : PlayerChunkTickets.AllocatingRateLimiterHandler {

    override fun previewAllocation(
        rateLimiter: Any,
        type: PlayerChunkTickets.AllocatingRateLimiterHandler.Type
    ): Long {
        rateLimiter as AllocatingRateLimiter
        val maxRate = getMaxRate(type)
        return rateLimiter.previewAllocation(System.nanoTime(), maxRate, maxRate.toLong())
    }

}