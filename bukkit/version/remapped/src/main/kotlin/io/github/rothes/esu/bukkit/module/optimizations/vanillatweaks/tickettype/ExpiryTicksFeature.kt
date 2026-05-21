/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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