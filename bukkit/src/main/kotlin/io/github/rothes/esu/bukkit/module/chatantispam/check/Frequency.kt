package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType

object Frequency: Check() {

    override fun check(request: MessageRequest): CheckResult {
        val spamData = request.spamData
        val time = request.sendTime
        with(request.spamCheck.frequency) {
            spamData.records.lastOrNull()?.let {
                if (minimalInterval >= time - it.time) {
                    return CheckResult("freq iv", 0.15,
                            addFilter = request.messageMeta.type != MessageType.DEATH)
                }
            }
            val times = spamData.records.reversed().indexOfFirst { time - it.time > maxMessagesPer }.let {
                if (it == -1) spamData.records.size else it
            }
            if (maxMessages in 1..times) {
                return CheckResult("freq", 0.2,
                    addFilter = request.messageMeta.type != MessageType.DEATH)
            }
        }
        return CheckResult()
    }
}