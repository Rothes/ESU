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

package io.github.rothes.esu.bukkit.util.version.adapter.nms

import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.core.EsuBootstrap
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

interface PlayerEntityVisibilityHandler {

    fun showEntity(player: Player, bukkitEntity: Entity, plugin: Plugin = EsuBootstrap.instance as Plugin) {
        if (bukkitEntity.checkTickThread()) { // Entity.isVisibleByDefault() calls getHandle() which may check tickThread
            player.showEntity(plugin, bukkitEntity)
        } else {
            forceShowEntity(player, bukkitEntity, plugin)
        }
    }

    fun forceShowEntity(player: Player, bukkitEntity: Entity, plugin: Plugin = EsuBootstrap.instance as Plugin)

}