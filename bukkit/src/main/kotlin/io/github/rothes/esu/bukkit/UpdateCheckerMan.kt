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

import io.github.rothes.esu.bukkit.user.ConsoleUser
import io.github.rothes.esu.core.EsuCore
import io.github.rothes.esu.core.user.User
import io.github.rothes.esu.core.util.UpdateChecker
import io.github.rothes.esu.core.util.UpdateChecker.VersionAction
import io.github.rothes.esu.data.BuildInfo
import org.bukkit.Bukkit
import java.util.*

object UpdateCheckerMan {

    private val checker =
        UpdateChecker(
            BuildInfo.VERSION_ID.toInt(),
            BuildInfo.VERSION_CHANNEL,
            BuildInfo.PLUGIN_PLATFORM,
            ConsoleUser,
            EnumMap<VersionAction, () -> Unit>(VersionAction::class.java).apply {
                put(VersionAction.PROHIBIT) { Bukkit.getPluginManager().disablePlugin(plugin) }
            },
            { Bukkit.getOnlinePlayers().map { it.user } },
            EsuCore.instance.basePermissionNode
        )

    fun reload() {
        checker.onReload()
    }

    fun shutdown() {
        checker.shutdown()
    }

    fun onJoin(user: User) {
        checker.onJoin(user)
    }

}