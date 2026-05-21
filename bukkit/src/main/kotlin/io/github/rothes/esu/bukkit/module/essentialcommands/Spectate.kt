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

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.ComponentBukkitUtils.player
import io.github.rothes.esu.bukkit.util.ServerCompatibility.tp
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.onTick
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.onTickDeferred
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.core.command.annotation.ShortPerm
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.module.configuration.FeatureToggle
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command

object Spectate: BaseCommand<FeatureToggle.DefaultTrue, Spectate.Lang>() {

    override fun onEnable() {
        registerCommands(object {
            @Command("spectate <target>")
            @ShortPerm
            suspend fun spectate(sender: PlayerUser, @Argument("target") target: Player) {
                val caller = sender.player
                if (caller == target) {
                    return sender.message(lang, { cannotSpectateSelf })
                }
                if (caller.gameMode != GameMode.SPECTATOR) {
                    caller.onTickDeferred {
                        caller.gameMode = GameMode.SPECTATOR
                    }.join()
                }
                target.onTick {
                    if (caller.checkTickThread())
                        caller.spectatorTarget = target
                    else {
                        caller.tp(target.location) {
                            caller.spectatorTarget = target
                        }
                    }
                    sender.message(lang, { spectatingTarget }, player(target, "target"))
                }
            }
        }) { parser ->
            parser.registerBuilderDecorator {
                it.senderType(PlayerUser::class.java)
            }
        }
    }

    data class Lang(
        val cannotSpectateSelf: MessageData = "<ec>You cannot spectate yourself!".message,
        val spectatingTarget: MessageData = "<pc>Spectating <pdc><target></pdc> now.".message,
    )

}