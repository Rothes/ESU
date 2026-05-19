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
import io.github.rothes.esu.core.util.extension.DurationExt.compareTo
import kotlin.math.abs

object FixedRequest: Check("fixed-request") {

    override val defaultBlockedMessage = "<ec>We detected a suspicious bot chat activity.".message

    override fun check(request: MessageRequest): CheckResult {
        if (!request.context.createdByOwn) {
            return CheckResult()
        }

        val check = request.spamCheck.fixedRequestMute
        if (check.enabled) {
            val spamData = request.spamData
            check.conditions.forEach { condition ->
                val samples = condition.samples
                val interval = condition.minRequestInterval
                val drift = condition.drift

                if (spamData.requests.size >= samples && samples >= 3) {
                    var time = request.sendTime
                    val req = spamData.requests.takeLast(samples).reversed().drop(1).map { send ->
                        (time - send).also { time = send }
                    }
                    val avg = req.average()
                    if (interval <= avg && req.indexOfFirst { abs(avg - it) > drift } == -1) {
                        notifyBlocked(request.user)
                        return CheckResult("fixed", 1.0, addFilter = false, mute = true)
                    }
                }
            }
        }
        return CheckResult()
    }
}