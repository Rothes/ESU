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

package io.github.rothes.esu.bukkit.module.essentialcommands

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player

object Feed : PlayerOptionalCommand<FeatureToggle.DefaultTrue, Feed.Lang>() {

    override fun onPerform(sender: User, player: Player, silent: Boolean) {
        player.syncTick {
            player.foodLevel = 20
            player.saturation = 20.0f // 5.0f on respawn
            player.exhaustion = 0.0f
            sender.message(lang, { fedPlayer }, player(player))
            if (!silent) {
                player.user.message(lang, { fed })
            }
        }
    }

    data class Lang(
        val fed: MessageData = "<pc>You have been fed.".message,
        val fedPlayer: MessageData = "<pc>Fed <pdc><player></pc>.".message,
    )
}