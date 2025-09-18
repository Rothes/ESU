package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import kotlin.collections.any

object IllegalCharacters: Check("illegal-characters") {

    override val defaultBlockedMessage = "<ec>Your message contains illegal characters.".message

    override fun check(request: MessageRequest): CheckResult {
        if (request.message.any { it == '\u202e' || it == '\u202c' || it == '\u202D' }) {
            notifyBlocked(request.user)
            return CheckResult(filter = "bad char", 0.187)
        }
        return CheckResult()
    }
}