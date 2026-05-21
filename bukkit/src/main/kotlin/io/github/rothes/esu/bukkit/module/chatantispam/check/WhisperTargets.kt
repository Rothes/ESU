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

import io.github.rothes.esu.bukkit.module.chatantispam.CasListeners.sizedAdd
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.meta.WhisperMessage
import io.github.rothes.esu.bukkit.module.chatantispam.user.SpamData
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message

object WhisperTargets: Check("whisper-targets") {

    override val defaultBlockedMessage = "<ec>You are whispering too many players!".message

    override fun check(request: MessageRequest): CheckResult {
        val messageMeta = request.context
        if (messageMeta !is WhisperMessage)
            return CheckResult()

        val conf = config.whisperTargets

        val spamData = request.spamData
        val time = request.sendTime

        val receiver = messageMeta.receiver
        val index = spamData.whisperTargets.indexOfFirst { it.player == receiver }
        if (index != -1) {
            if (conf.safeTargets > index && spamData.whisperTargets.size <= conf.safeTargetsMax) {
                spamData.whisperTargets[index].lastTime = time
                return CheckResult(endChecks = true, score = -1.0)
            } else if (spamData.muteUntil <= time) {
                // Not muted
                if (index >= conf.maxTargets) {
                    notifyBlocked(request.user)
                    return CheckResult("wide msg", 0.25)
                }
                spamData.whisperTargets[index].lastTime = time
            }
        } else {
            spamData.whisperTargets.sizedAdd(config.expireSize.whisperTarget, SpamData.WhisperTarget(receiver, time))
            if (conf.safeTargets > spamData.whisperTargets.size + 1) {
                return CheckResult(endChecks = true, score = -1.0)
            } else if (spamData.muteUntil <= time) {
                if (spamData.whisperTargets.size >= conf.maxTargets) {
                    notifyBlocked(request.user)
                    return CheckResult("wide msg", 0.25)
                }
            }
        }
        return CheckResult()
    }
}