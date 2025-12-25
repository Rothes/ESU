package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks

import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.ChunkLoadsFeature
import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.ChunkUnloadsFeature
import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.ExpiryTicksFeature
import io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype.TicketTypeCommandsFeature
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.configuration.meta.NoDeserializeNull
import io.github.rothes.esu.core.configuration.meta.RemovedNode
import io.github.rothes.esu.core.module.CommonFeature

object TicketTypeFeatures: CommonFeature<TicketTypeFeatures.FeatureConfig, Unit>() {

    init {
        registerFeature(TicketTypeCommandsFeature)
        registerFeature(ExpiryTicksFeature)
        if (ServerCompatibility.serverVersion >= "21.8") registerFeature(ChunkLoadsFeature) // TODO is it added before 1.21.8?
        if (ServerCompatibility.serverVersion >= "21.9") registerFeature(ChunkUnloadsFeature)
    }

    override val name: String = "TicketType"

    override fun onEnable() { }

    @Comment("""
        Change server ticket type settings.
        Tickets control chunk loading. For details, check https://minecraft.wiki/w/Chunk#Tickets
    """)
    class FeatureConfig: ConfigurationPart {
        @RemovedNode("0.14.1") val enabled: Boolean? = null
        @NoDeserializeNull
        @RemovedNode("0.14.1") val startupSettings: Map<String, Int>? = null
    }

}