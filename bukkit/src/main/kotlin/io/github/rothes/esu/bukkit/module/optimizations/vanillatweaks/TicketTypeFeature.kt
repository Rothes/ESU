package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.CommonFeature
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration

object TicketTypeFeature: CommonFeature<TicketTypeFeature.FeatureConfig, Unit>() {

    override fun onEnable() {
        applyTicketType()
    }

    override fun onReload() {
        super.onReload()
        if (enabled)
            applyTicketType()
    }

    private fun applyTicketType() {
        for ((key, value) in config.startupSettings) {
            val ticketType = TicketTypeHandler.handler.getTicketTypeMap()[key]
            if (ticketType == null) {
                core.err("[$name] $key ticket type not found")
                continue
            }
            ticketType.expiryTicks = value
        }
    }

    interface TicketTypeHandler {

        fun getTicketTypeMap(): Map<String, TicketType>

        interface TicketType {
            val handle: Any
            val name: String
            var expiryTicks: Long
        }

        companion object {
            val handler by Versioned(TicketTypeHandler::class.java)
        }
    }

    @Comment("""
        Change server ticket type settings.
        Tickets control chunk loading. For details, check https://minecraft.wiki/w/Chunk#Tickets
        Setting expiry-ticks to 0 or negative value makes the chunk load forever until the ticket
         is manually removed.
    """)
    data class FeatureConfig(
        @Comment("""
            Change the expiry ticks value once the module is enabled or reloaded.
            For example, if you set `portal: 1`, chunk loaded by portal teleport will be removed
             right after 1 game tick, so that "chunk loader" will not be working any more.
        """)
        val startupSettings: Map<String, Long> = TicketTypeHandler.handler.getTicketTypeMap().mapValues { it.value.expiryTicks }
    ): BaseFeatureConfiguration()
}