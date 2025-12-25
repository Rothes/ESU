package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.ExpiryTicksFeature
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.configuration.meta.RemovedNode
import io.github.rothes.esu.core.module.CommonFeature

object TicketTypeFeatures: CommonFeature<TicketTypeFeatures.FeatureConfig, Unit>() {

    init {
        registerFeature(ExpiryTicksFeature)
    }

    override val name: String = "TicketType"

    override fun onEnable() { }

    @Comment("""
        Change server ticket type settings.
        Tickets control chunk loading. For details, check https://minecraft.wiki/w/Chunk#Tickets
    """)
    class FeatureConfig: ConfigurationPart {
        @RemovedNode("0.14.1") val enabled: Boolean = true
        @RemovedNode("0.14.1") val startupSettings: Map<String, Int> = mapOf()
    }

}