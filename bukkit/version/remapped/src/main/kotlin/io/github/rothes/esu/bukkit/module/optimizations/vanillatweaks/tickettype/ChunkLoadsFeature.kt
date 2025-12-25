package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.Feature
import io.github.rothes.esu.core.module.Feature.AvailableCheck.Companion.errFail
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration

object ChunkLoadsFeature: BaseTicketTypeFeature<ChunkLoadsFeature.FeatureConfig, Unit>() {

    override fun checkUnavailable(): Feature.AvailableCheck? {
        return super.checkUnavailable() ?: let {
            if (ServerCompatibility.isPaper) {
                // Moonrise chunk system rewrite does not respect #doesSimulate()
                return errFail { "This feature doesn't work on Paper.".message }
            }
            null
        }
    }

    override fun apply() {
        val allowNothing = ServerCompatibility.serverVersion >= "21.9"
        with(advancedHandler) {
            for ((key, value) in config.overrides) {
                if (!allowNothing && !value.loadsChunk && !value.ticksChunk) {
                    core.err("[$name] Attempted to set ticket type $key no loadsChunk either ticksChunk, but this requires Minecraft 1.21.9+ !")
                    continue
                }
                val handle = findTicketType(key)?.handle ?: continue
                handle.persist = value.persist
                handle.loadsChunk = value.loadsChunk
                handle.ticksChunk = value.ticksChunk
                println("$key ${handle.doesLoad()} ${handle.doesSimulate()}")
            }
        }
    }

    @Comment("""
        Overrides the chunk loads level of ticket types.
    """)
    data class FeatureConfig(
        val overrides: Map<String, LoadSettings> = TicketTypeHandler.handler.getTicketTypeMap().mapValues {
            with(advancedHandler) {
                LoadSettings(it.value.handle.persist, it.value.handle.loadsChunk, it.value.handle.ticksChunk)
            }
        }
    ): BaseFeatureConfiguration() {

        data class LoadSettings(
            val persist: Boolean = false,
            val loadsChunk: Boolean = true,
            val ticksChunk: Boolean = false,
        )
    }

}