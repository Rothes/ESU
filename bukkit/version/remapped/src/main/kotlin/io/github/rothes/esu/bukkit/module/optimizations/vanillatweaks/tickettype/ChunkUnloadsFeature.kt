package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration

object ChunkUnloadsFeature: BaseTicketTypeFeature<ChunkUnloadsFeature.FeatureConfig, Unit>() {

    override fun apply() {
        with(advancedHandler) {
            for ((key, value) in config.overrides) {
                val handle = findTicketType(key)?.handle ?: continue
                handle.keepsDimensionActive = value.keepsDimensionActive
                handle.expiresIfUnloaded = value.expiresIfUnloaded
            }
        }
    }

    @Comment("""
        Introduces more customizes since Minecraft 1.21.9 .
    """)
    data class FeatureConfig(
        val overrides: Map<String, LoadSettings> = TicketTypeHandler.handler.getTicketTypeMap().mapValues {
            with(advancedHandler) {
                LoadSettings(it.value.handle.keepsDimensionActive, it.value.handle.expiresIfUnloaded)
            }
        }
    ): BaseFeatureConfiguration() {

        data class LoadSettings(
            val keepsDimensionActive: Boolean = true,
            val expiresIfUnloaded: Boolean = true,
        )
    }

}