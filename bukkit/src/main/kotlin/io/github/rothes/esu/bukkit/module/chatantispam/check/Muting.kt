package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType

object Muting: Check() {

    override fun check(request: MessageRequest): CheckResult {
        if (request.messageMeta.type == MessageType.DEATH) {
            return CheckResult()
        }
        if (request.spamData.muteUntil > request.sendTime) {
            return CheckResult(block = true, score = -1.0)
        }
        return CheckResult()
    }
}