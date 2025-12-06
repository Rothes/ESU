package io.github.rothes.esu.bukkit.module.chatantispam.message

import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule
import io.github.rothes.esu.bukkit.module.chatantispam.user.SpamData
import io.github.rothes.esu.bukkit.user
import org.bukkit.entity.Player

data class MessageRequest(
    val player: Player,
    val messageMeta: MessageMeta,
    val spamCheck: ChatAntiSpamModule.ModuleConfig.SpamCheck,
    val spamData: SpamData,
    val sendTime: Long,
    val afkTime: Long,
    val rawMessage: String,
    var message: String = rawMessage
) {
    val user by lazy { player.user }
}
