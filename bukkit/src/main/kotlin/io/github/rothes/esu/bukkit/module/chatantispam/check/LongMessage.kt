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

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.util.extension.charSize
import kotlin.math.max
import kotlin.math.min

object LongMessage: Check("long-message") {

    override val defaultBlockedMessage = "<ec>Your message is too long!".message

    override fun check(request: MessageRequest): CheckResult {
        val maxMessageSize = request.spamCheck.longMessage.maxMessageSize
        val length = request.message.charSize()
        if (maxMessageSize in 0..< length) {
            notifyBlocked(request.user)
            return CheckResult("long", max(0.2, min((length - maxMessageSize) * 0.015, 0.6)))
        }
        return CheckResult()
    }

}