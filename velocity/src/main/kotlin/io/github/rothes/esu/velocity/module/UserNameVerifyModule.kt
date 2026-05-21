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

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PostLoginEvent
import io.github.rothes.esu.core.configuration.ConfigurationPart
import io.github.rothes.esu.core.module.configuration.BaseModuleConfiguration
import io.github.rothes.esu.core.util.ComponentUtils.component
import io.github.rothes.esu.velocity.user

object UserNameVerifyModule: VelocityModule<UserNameVerifyModule.ModuleConfig, UserNameVerifyModule.ModuleLang>() {

    override fun onEnable() {
        registerListener(Listener)
    }

    object Listener {

        @Subscribe(order = PostOrder.FIRST)
        fun onLogin(e: PostLoginEvent) {
            val username = e.player.username
            for ((key, regex) in config.requirements) {
                if (regex.matchEntire(username) == null) {
                    val user = e.player.user
                    user.awaitSettings(500) // Wait for client locale configuration
                    user.kick(lang, { kickMessage.messages[key] },
                        component("prefix", user.buildMiniMessage(lang, { kickMessage.prefix }))
                    )
                    break
                }
            }
        }
    }

    data class ModuleLang(
        val kickMessage: KickMessage = KickMessage(),
    ): ConfigurationPart {

        data class KickMessage(
            val prefix: String = "<pc>You have failed username verification: <br><br>",
            val messages: Map<String, String> = linkedMapOf(
                "length" to "<prefix>" +
                        "<pdc>Username length must in 3-16",
                "ascii" to "<prefix>" +
                        "<pdc>Only alphanumeric characters (letters and numbers)<br>" +
                        "and underscores (_) are allowed in username",
            ),
        )
    }

    data class ModuleConfig(
        val requirements: Map<String, Regex> = linkedMapOf(
            "length" to "^.{3,16}$".toRegex(),
            "ascii" to "^[a-zA-Z0-9_]+$".toRegex(),
        ),
    ): BaseModuleConfiguration()

}