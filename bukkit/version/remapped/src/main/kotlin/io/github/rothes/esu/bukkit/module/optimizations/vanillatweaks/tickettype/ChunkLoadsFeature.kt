/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.module.optimizations.vanillatweaks.tickettype

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.configuration.meta.Comment
import io.github.rothes.esu.core.module.configuration.BaseFeatureConfiguration

object ChunkLoadsFeature: BaseTicketTypeFeature<ChunkLoadsFeature.FeatureConfig, Unit>() {

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
            }
        }
    }

    @Comment("""
        Overrides the chunk loads options of ticket types.
        Changing loads-chunk and ticks-chunk on Paper has no use, due to Moonrise chunk system optimization.
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