package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import kotlin.math.max
import kotlin.math.min

object LongMessage: Check() {

    override fun check(request: MessageRequest): CheckResult {
        val maxMessageSize = request.spamCheck.longMessage.maxMessageSize
        val length = request.message.sumOf { max(it.toString().toByteArray().size, 2) }
        if (maxMessageSize in 0..length) {
            return CheckResult("long", max(0.2, min((length - maxMessageSize) * 0.015, 0.6)))
        }
        return CheckResult()
    }

}