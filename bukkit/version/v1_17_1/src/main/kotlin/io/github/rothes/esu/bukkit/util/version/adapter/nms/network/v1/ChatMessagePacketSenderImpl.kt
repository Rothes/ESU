package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v1

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.ChatMessagePacketSender
import io.github.rothes.esu.lib.adventure.chat.ChatType
import io.github.rothes.esu.lib.adventure.chat.SignedMessage
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundChatPacket
import org.bukkit.entity.Player
import net.minecraft.network.chat.ChatType as MinecraftChatType

object ChatMessagePacketSenderImpl: ChatMessagePacketSender {

    override fun deleteMessage(player: Player, signature: SignedMessage.Signature) {
        // Not supported server
    }

    override fun sendSystemMessage(player: Player, message: Component) {
        player.handle.connection.send(ClientboundChatPacket(message.toMinecraft(), MinecraftChatType.CHAT, null))
    }

    override fun sendMessage(player: Player, message: Component, boundChatType: ChatType.Bound) {
        sendSystemMessage(player, message)
    }

//    override fun sendMessage(player: Player, signedMessage: SignedMessage, boundChatType: ChatType.Bound) {
//        val msg = signedMessage.unsignedContent() ?: Component.text(signedMessage.message())
//        player.handle.connection.send(ClientboundChatPacket(msg.toMinecraft(), MinecraftChatType.CHAT, null))
//    }

}