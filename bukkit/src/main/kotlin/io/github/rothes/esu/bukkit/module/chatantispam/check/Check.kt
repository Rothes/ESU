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

package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.core.configuration.data.MessageData
import io.github.rothes.esu.lib.adventure.text.minimessage.tag.resolver.TagResolver

abstract class Check(val type: String) {

    protected val config: ChatAntiSpamModule.ModuleConfig
        get() = ChatAntiSpamModule.config

    abstract fun check(request: MessageRequest): CheckResult

    fun notifyBlocked(user: PlayerUser, vararg params: TagResolver) {
        user.message(ChatAntiSpamModule.lang, { blockedMessage[type] }, *params)
    }

    open val defaultBlockedMessage: MessageData? = null

    data class CheckResult(
        val filter: String? = null,
        val score: Double = 0.0,
        val mergeScore: Boolean = true,
        val notify: Boolean? = null,
        val addFilter: Boolean = true,
        val mute: Boolean = false,
        val block: Boolean = filter != null,
        val endChecks: Boolean = block,
    )
}