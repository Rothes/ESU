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

package io.github.rothes.esu.bukkit

import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.EsuBootstrap
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.lib.adventure.audience.Audience
import io.github.rothes.esu.lib.adventure.platform.bukkit.BukkitAudiences
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.*

val plugin: Plugin
    get() = EsuBootstrap.instance as Plugin

val core: EsuCoreBukkit
    get() = EsuCore.instance as EsuCoreBukkit

val adventure = BukkitAudiences.create(plugin)

val Player.user: PlayerUser
    get() = BukkitUserManager[this]
val UUID.playerUser: PlayerUser
    get() = BukkitUserManager[this]

val CommandSender.audience: Audience
    get() = adventure.sender(this)