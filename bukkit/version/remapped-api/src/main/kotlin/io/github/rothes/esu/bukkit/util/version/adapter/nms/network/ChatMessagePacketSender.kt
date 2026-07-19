package io.github.rothes.esu.bukkit.util.version.adapter.nms.network

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.lib.adventure.chat.ChatType
import io.github.rothes.esu.lib.adventure.chat.SignedMessage
import io.github.rothes.esu.lib.adventure.text.Component
import org.bukkit.entity.Player

interface ChatMessagePacketSender {

    fun deleteMessage(player: Player, signature: SignedMessage.Signature)
    fun sendSystemMessage(player: Player, message: Component)
    fun sendMessage(player: Player, message: Component, boundChatType: ChatType.Bound)
//    fun sendMessage(player: Player, signedMessage: SignedMessage, boundChatType: ChatType.Bound)

    companion object {

        val INSTANCE = versioned<ChatMessagePacketSender>()

    }

}