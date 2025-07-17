package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import io.github.rothes.esu.core.configuration.data.MessageData.Companion.message
import io.github.rothes.esu.core.util.ComponentUtils.duration
import kotlin.time.Duration.Companion.milliseconds

object Muting: Check("muting") {

    override val defaultBlockedMessage = "<ec>You are getting muted temporarily. Please wait <edc><duration><ec>, and behave better.<br>You can still whisper some of your friends now.".message

    override fun check(request: MessageRequest): CheckResult {
        if (request.messageMeta.type == MessageType.DEATH) {
            return CheckResult()
        }
        if (request.spamData.muteUntil > request.sendTime) {
            notifyBlocked(request.user,
                duration((request.spamData.muteUntil - request.sendTime).milliseconds, request.user)
            )
            return CheckResult(block = true, score = -1.0)
        }
        return CheckResult()
    }
}