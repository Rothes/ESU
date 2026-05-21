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

package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.util.extension.DurationExt.compareTo

object Frequency: Check("frequency") {

    override val defaultBlockedMessage = "<ec>You are sending messages too frequently.".message

    override fun check(request: MessageRequest): CheckResult {
        val spamData = request.spamData
        val time = request.sendTime
        with(request.spamCheck.frequency) {
            spamData.records.lastOrNull()?.let {
                if (minimalInterval >= time - it.time) {
                    val notify = request.context.createdByOwn
                    if (notify)
                        notifyBlocked(request.user)
                    return CheckResult("freq iv", 0.15,
                            addFilter = notify)
                }
            }
            val times = spamData.records.reversed().indexOfFirst { time - it.time > maxMessagesPer }.let {
                if (it == -1) spamData.records.size else it
            }
            if (maxMessages in 1..times) {
                val notify = request.context.createdByOwn
                if (notify)
                    notifyBlocked(request.user)
                return CheckResult("freq", 0.2,
                    addFilter = notify)
            }
        }
        return CheckResult()
    }
}