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

package io.github.rothes.esu.bukkit.module

import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.bukkit.util.scheduler.Scheduler
import io.github.rothes.esu.common.module.AbstractAutoRestartModule
import io.github.rothes.esu.core.user.User
import org.bukkit.Bukkit

object AutoRestartModule: AbstractAutoRestartModule() {

    override val consoleUser: User = ConsoleUser
    override val rootCommand: String = "autoRestart"
    override val rootCommandAlias: String = "ar"

    override fun runCommands() {
        Scheduler.global(2) { // Make sure all players are disconnected on their region thread
            config.commands.forEach {
                Bukkit.dispatchCommand(ConsoleUser.commandSender, it)
            }
        }
    }

}