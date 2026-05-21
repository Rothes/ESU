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

package io.github.rothes.esu.bukkit.module.core

import org.bukkit.entity.Player

interface PlayerTimeProvider {

    operator fun get(player: Player): Long
    operator fun set(player: Player, time: Long)

    fun registerListener(listener: ChangeListener)
    fun unregisterListener(listener: ChangeListener)

    interface ChangeListener {
        fun onTimeChanged(player: Player, oldTime: Long, newTime: Long)
    }

}