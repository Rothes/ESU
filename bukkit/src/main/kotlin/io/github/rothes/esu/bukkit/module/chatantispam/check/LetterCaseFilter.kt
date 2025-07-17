package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest

object LetterCaseFilter: Check("letter-case-filter") {

    override fun check(request: MessageRequest): CheckResult {
        if (request.spamCheck.letterCase.uniformOnCheck) {
            request.message = request.message.lowercase()
        }
        return CheckResult()
    }
}