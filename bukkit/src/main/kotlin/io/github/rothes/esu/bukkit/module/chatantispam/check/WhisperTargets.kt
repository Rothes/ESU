package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.CasListeners.sizedAdd
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import io.github.rothes.esu.bukkit.module.chatantispam.user.SpamData

object WhisperTargets: Check() {

    override fun check(request: MessageRequest): CheckResult {
        val messageMeta = request.messageMeta
        if (messageMeta.type != MessageType.WHISPER)
            return CheckResult()

        val spamData = request.spamData
        val time = request.sendTime

        val receiver = messageMeta.receiver!!
        val index = spamData.whisperTargets.indexOfFirst { it.player == receiver }
        if (index != -1) {
            if (config.whisperTargets.safeTargets > index) {
                spamData.whisperTargets[index].lastTime = time
                return CheckResult(endChecks = true, score = -1.0)
            } else if (spamData.muteUntil <= time) {
                // Not muted
                spamData.whisperTargets[index].lastTime = time
            }
        } else {
            if (config.whisperTargets.safeTargets > spamData.whisperTargets.size + 1) {
                spamData.whisperTargets.sizedAdd(config.expireSize.whisperTarget, SpamData.WhisperTarget(receiver, time))
                return CheckResult(endChecks = true, score = -1.0)
            } else if (spamData.muteUntil <= time) {
                spamData.whisperTargets.sizedAdd(config.expireSize.whisperTarget, SpamData.WhisperTarget(receiver, time))
                if (spamData.whisperTargets.size >= config.whisperTargets.maxTargets) {
                    return CheckResult("wide msg", 0.25)
                }
            }
        }
        return CheckResult()
    }
}