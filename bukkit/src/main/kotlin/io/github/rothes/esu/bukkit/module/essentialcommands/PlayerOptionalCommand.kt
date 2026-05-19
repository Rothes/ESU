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

import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.user.User
import org.bukkit.entity.Player
import org.incendo.cloud.bukkit.parser.PlayerParser
import org.incendo.cloud.kotlin.extension.commandBuilder

abstract class PlayerOptionalCommand<C, L>: BaseCommand<C, L>() {

    protected open val receivesSilentFlag: Boolean
        get() = true
    protected open val aliases: Array<String>
        get() = arrayOf()

    override fun onEnable() {
        withCommandManager {
            commandBuilder(name, aliases = aliases) {
                copy {
                    permission(cmdShortPerm())
                    senderType(PlayerUser::class.java)
                    handler { ctx ->
                        val sender = ctx.sender() as PlayerUser
                        onPerform(sender, sender.player, true)
                    }
                    regCmd()
                }
                permission(cmdShortPerm("others"))
                required("player", PlayerParser.playerParser())
                if (receivesSilentFlag) flag("silent")
                handler { ctx ->
                    val sender = ctx.sender()
                    val player = ctx.get<Player>("player")
                    val silent = ctx.flags().isPresent("silent")
                    onPerform(sender, player, silent || sender is PlayerUser && sender.player == player)
                }
                regCmd()
            }
        }
    }

    abstract fun onPerform(sender: User, player: Player, silent: Boolean)

}