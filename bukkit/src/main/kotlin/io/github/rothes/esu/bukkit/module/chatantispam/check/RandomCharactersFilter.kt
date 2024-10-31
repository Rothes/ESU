package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest

object RandomCharactersFilter: Check() {

    override fun check(request: MessageRequest): CheckResult {
        if (request.spamCheck.randomCharacters.removeRandomCharactersOnCheck) {
            request.message = request.message.filterNot {
                val char = it.code
                char < 32 || char in 127..8527
                // 7424 - 7515; 8304~ : Fancy text
            }
        }
        return CheckResult()
    }
}