package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType

object SpacesFilter: Check("spaces-filter") {

    private val duplicateSpaceRegex = "[\\s\\u00A0\\u1680\\u180E\\u2000-\\u200B\\u202F\\u205F\\u3000\\uFEFF]+".toRegex()

    override fun check(request: MessageRequest): CheckResult {
        if (request.messageMeta.type == MessageType.DEATH) {
            return CheckResult()
        }
        val spacesCheck = request.spamCheck.spaces
        val message = request.message
        if (spacesCheck.minLength >= 0 && spacesCheck.minLength <= message.length) {
            val spaceCount = message.filter { it.isWhitespace() }.length
            val spaceRate = spaceCount / message.length.toDouble()
            if (spaceRate >= spacesCheck.spaceRate) {
                return CheckResult("spaces", 0.275)
            }
        }

        if (spacesCheck.removeExtraSpacesOnCheck) {
            request.message = request.message.replace(duplicateSpaceRegex, " ")
        }
        return CheckResult()
    }
}