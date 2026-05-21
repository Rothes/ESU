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
import io.github.rothes.esu.core.util.ComponentUtils.duration
import kotlin.time.Duration.Companion.milliseconds

object Muting: Check("muting") {

    override val defaultBlockedMessage = "<ec>You are getting muted temporarily. Please wait <edc><duration><ec>, and behave better.<br>You can still whisper some of your friends now.".message

    override fun check(request: MessageRequest): CheckResult {
        if (!request.context.createdByOwn) {
            return CheckResult()
        }
        if (request.spamData.muteUntil > request.sendTime) {
            notifyBlocked(request.user,
                duration((request.spamData.muteUntil - request.sendTime).milliseconds, request.user)
            )
            return CheckResult(block = true, score = -1.0)
        }
        return CheckResult()
    }
}