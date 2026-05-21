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

package io.github.rothes.esu.velocity.module

import io.github.rothes.esu.common.module.AbstractAutoRestartModule
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.velocity.plugin
import io.github.rothes.esu.velocity.user.ConsoleUser

object AutoRestartModule: AbstractAutoRestartModule() {

    override val consoleUser: User = ConsoleUser
    override val rootCommand: String = "vAutoRestart"
    override val rootCommandAlias: String = "var"

    override fun runCommands() {
        config.commands.forEach {
            plugin.server.commandManager.executeAsync(ConsoleUser.commandSender, it)
        }
    }
}