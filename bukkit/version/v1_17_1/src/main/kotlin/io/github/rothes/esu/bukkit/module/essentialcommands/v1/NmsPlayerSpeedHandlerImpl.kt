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

package io.github.rothes.esu.bukkit.module.essentialcommands.v1

import io.github.rothes.esu.bukkit.module.essentialcommands.Speed
import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer
import org.bukkit.entity.Player as BukkitPlayer

object NmsPlayerSpeedHandlerImpl: Speed.NmsPlayerSpeedHandler {

    override fun getWalkSpeed(bukkitPlayer: BukkitPlayer) = bukkitPlayer.nms.abilities.walkingSpeed
    override fun getFlySpeed(bukkitPlayer: BukkitPlayer) = bukkitPlayer.nms.abilities.flyingSpeed

    override fun setWalkSpeed(bukkitPlayer: BukkitPlayer, speed: Float) {
        val player = bukkitPlayer.nms
        player.abilities.walkingSpeed = speed
        player.onUpdateAbilities()
        bukkitPlayer.getAttribute(AttributeAdapter.MOVEMENT_SPEED)!!.baseValue = speed.toDouble()
    }
    override fun setFlySpeed(bukkitPlayer: BukkitPlayer, speed: Float) {
        val player = bukkitPlayer.nms
        player.abilities.flyingSpeed = speed
        player.onUpdateAbilities()
    }

    private val BukkitPlayer.nms
        get() = (this as CraftPlayer).handle
}