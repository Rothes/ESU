package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.util.extension.DurationExt.compareTo

object Frequency: Check("frequency") {

    override val defaultBlockedMessage = "<ec>You are sending messages to frequently.".message

    override fun check(request: MessageRequest): CheckResult {
        val spamData = request.spamData
        val time = request.sendTime
        with(request.spamCheck.frequency) {
            spamData.records.lastOrNull()?.let {
                if (minimalInterval >= time - it.time) {
                    val notify = request.messageMeta.type != MessageType.DEATH
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
                val notify = request.messageMeta.type != MessageType.DEATH
                if (notify)
                    notifyBlocked(request.user)
                return CheckResult("freq", 0.2,
                    addFilter = notify)
            }
        }
        return CheckResult()
    }
}