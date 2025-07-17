package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import kotlin.math.max
import kotlin.math.min

object LongMessage: Check("long-message") {

    override val defaultBlockedMessage = "<ec>Your message is too long!".message

    override fun check(request: MessageRequest): CheckResult {
        val maxMessageSize = request.spamCheck.longMessage.maxMessageSize
        val length = request.message.sumOf { max(it.toString().toByteArray().size, 2) }
        if (maxMessageSize in 0..length) {
            notifyBlocked(request.user)
            return CheckResult("long", max(0.2, min((length - maxMessageSize) * 0.015, 0.6)))
        }
        return CheckResult()
    }

}