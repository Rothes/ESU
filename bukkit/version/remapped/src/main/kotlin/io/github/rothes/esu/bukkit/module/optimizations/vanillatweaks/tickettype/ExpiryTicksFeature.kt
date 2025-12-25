package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.TicketTypeHandler
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration

object ExpiryTicksFeature: CommonFeature<ExpiryTicksFeature.FeatureConfig, Unit>() {

    private var previousSettingHash: Int = 0

    override fun onEnable() {
        applyTicketType()
    }

    override fun onReload() {
        super.onReload()
        if (enabled)
            applyTicketType()
    }

    private fun applyTicketType() {
        val config = config
        if (previousSettingHash == config.hashCode()) return
        for ((key, value) in config.ticketType) {
            val ticketType = TicketTypeHandler.handler.getTicketTypeMap()[key]
            if (ticketType == null) {
                core.err("[$name] $key ticket type not found")
                continue
            }
            ticketType.expiryTicks = value
        }
        previousSettingHash = config.hashCode()
    }

    @Comment("""
        Customize the expiry ticks of ticket types.
        Set expiry-ticks to 0 or negative value makes the chunk load forever until the ticket
         is manually removed.
    """)
    data class FeatureConfig(
        val ticketType: Map<String, Long> = TicketTypeHandler.handler.getTicketTypeMap().mapValues { it.value.expiryTicks }
    ): BaseFeatureConfiguration()
}