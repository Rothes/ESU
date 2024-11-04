package io.github.rothes.esu.bukkit.module.chatantispam.check

import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule
import io.github.rothes.esu.bukkit.module.chatantispam.message.MessageRequest

abstract class Check {

    protected val config: ChatAntiSpamModule.ConfigData
        get() = ChatAntiSpamModule.config

    abstract fun check(request: MessageRequest): CheckResult

    data class CheckResult(
        val filter: String? = null,
        val score: Double = 0.0,
        val mergeScore: Boolean = true,
        val notify: Boolean? = null,
        val addFilter: Boolean = true,
        val mute: Boolean = false,
        val block: Boolean = filter != null,
        val endChecks: Boolean = block,
    )
}