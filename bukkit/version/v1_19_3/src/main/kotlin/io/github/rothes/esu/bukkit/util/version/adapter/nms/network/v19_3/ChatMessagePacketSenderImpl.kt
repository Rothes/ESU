package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v19_3

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.ChatMessagePacketSender
import io.github.rothes.esu.lib.adventure.chat.ChatType
import io.github.rothes.esu.lib.adventure.chat.SignedMessage
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.network.chat.MessageSignature
import net.minecraft.network.chat.OutgoingChatMessage
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import org.bukkit.entity.Player

object ChatMessagePacketSenderImpl: ChatMessagePacketSender {

    override fun deleteMessage(player: Player, signature: SignedMessage.Signature) {
        player.handle.connection ?: return
        val m = MessageSignature(signature.bytes())
        player.handle.connection.send(ClientboundDeleteChatPacket(MessageSignature.Packed(m)))
    }

    override fun sendSystemMessage(player: Player, message: Component) {
        player.handle.connection.send(ClientboundSystemChatPacket(message.toMinecraft(), false))
    }

    override fun sendMessage(player: Player, message: Component, boundChatType: ChatType.Bound) {
        player.handle.sendChatMessage(OutgoingChatMessage.Disguised(message.toMinecraft()), player.handle.isTextFilteringEnabled, boundChatType.toMinecraft())
    }

}