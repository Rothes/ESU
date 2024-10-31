package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest

object IllegalCharacters: Check() {

    override fun check(request: MessageRequest): CheckResult {
        if (request.message.find { it == '\u202e' || it == '\u202c' || it == '\u202D' } != null) {
            return CheckResult(filter = "bad char", 0.187)
        }
        return CheckResult()
    }
}