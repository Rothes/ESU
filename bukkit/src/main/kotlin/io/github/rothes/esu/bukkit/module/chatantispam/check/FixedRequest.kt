package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageType
import kotlin.math.abs

object FixedRequest: Check() {

    override fun check(request: MessageRequest): CheckResult {
        if (request.messageMeta.type == MessageType.DEATH) {
            return CheckResult()
        }

        val check = request.spamCheck.fixedRequestMute
        if (check.enabled) {
            val spamData = request.spamData
            check.conditions.forEach { condition ->
                val samples = condition.samples
                val interval = condition.minRequestInterval
                val drift = condition.drift

                if (spamData.requests.size >= samples && samples >= 3) {
                    var time = request.sendTime
                    val req = spamData.requests.takeLast(samples).reversed().drop(1).map { send ->
                        (time - send).also { time = send }
                    }
                    val avg = req.average()
                    if (interval <= avg && req.indexOfFirst { abs(avg - it) > drift } == -1) {
                        return CheckResult("fixed", 1.0, addFilter = false, mute = true)
                    }
                }
            }
        }
        return CheckResult()
    }
}