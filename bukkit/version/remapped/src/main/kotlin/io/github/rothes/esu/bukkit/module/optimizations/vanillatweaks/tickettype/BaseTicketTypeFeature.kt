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

import io.github.rothes.esu.bukkit.core
import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.core.module.CommonFeature

abstract class BaseTicketTypeFeature<C, L> : CommonFeature<C, L>() {

    private var previousSettingHash: Int = 0

    val advancedHandler
        get() = versioned<TicketTypeAdvancedHandler>()

    override fun onEnable() {
        applySettings()
    }

    override fun onReload() {
        super.onReload()
        if (enabled)
            applySettings()
    }

    abstract fun apply()

    private fun applySettings() {
        val config = config
        if (previousSettingHash == config.hashCode()) return
        apply()
        previousSettingHash = config.hashCode()
    }

    protected fun findTicketType(key: String): TicketTypeHandler.NmsTicketType? {
        val ticketType = TicketTypeHandler.handler.getTicketTypeMap()[key]
        if (ticketType == null) {
            core.err("[$name] Ticket type $key does not exists!")
            return null
        }
        return ticketType
    }

}