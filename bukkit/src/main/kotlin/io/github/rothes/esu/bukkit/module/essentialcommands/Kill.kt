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

package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command

object Kill : BaseCommand<FeatureToggle.DefaultTrue, Kill.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("kill <player>")
            @ShortPerm("others")
            fun kill(sender: User, player: Player) {
                player.syncTick {
                    player.health = 0.0
                    sender.message(lang, { killedPlayer }, player(player))
                }
            }
        })
    }

    data class Lang(
        val killedPlayer: MessageData = "<pc>Killed player <pdc><player></pdc> .".message,
    )

}