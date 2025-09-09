package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest
import kotlin.concurrent.atomics.ExperimentalAtomicApi

object ConsecutiveUnfilteredFilter: Check("consecutive-unfiltered-filter") {

    @OptIn(ExperimentalAtomicApi::class)
    override fun check(request: MessageRequest): CheckResult {
        if (config.consecutiveUnfilteredThreshold > 0) {
            if (request.spamData.consecutiveUnfiltered.addAndFetch(1) >= config.consecutiveUnfilteredThreshold) {
                request.spamData.consecutiveUnfiltered.store(0)
            }
        }
        if (request.spamCheck.letterCase.uniformOnCheck) {
            request.message = request.message.lowercase()
        }
        return CheckResult()
    }
}