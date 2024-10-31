package io.github.rothes.esu.bukkit.module.chatantispam.message

import io.github.rothes.esu.bukkit.module.chatantispam.ChatAntiSpamModule
import io.github.rothes.esu.bukkit.module.chatantispam.user.SpamData
import org.bukkit.entity.Player

data class MessageRequest(
    val player: Player,
    val messageMeta: MessageMeta,
    val spamCheck: ChatAntiSpamModule.ConfigData.SpamCheck,
    val spamData: SpamData,
    val sendTime: Long,
    val rawMessage: String,
    var message: String = rawMessage
)
