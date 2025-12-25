package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration

object ExpiryTicksFeature: BaseTicketTypeFeature<ExpiryTicksFeature.FeatureConfig, Unit>() {

    override fun apply() {
        for ((key, value) in config.overrides) {
            val ticketType = findTicketType(key) ?: continue
            ticketType.expiryTicks = value
        }
    }

    @Comment("""
        Overrides the expiry ticks (timeout) of ticket types.
        Set to 0 or negative value makes the chunk load forever until the ticket
         is manually removed.
    """)
    data class FeatureConfig(
        val overrides: Map<String, Long> = TicketTypeHandler.handler.getTicketTypeMap().mapValues { it.value.expiryTicks }
    ): BaseFeatureConfiguration()

}