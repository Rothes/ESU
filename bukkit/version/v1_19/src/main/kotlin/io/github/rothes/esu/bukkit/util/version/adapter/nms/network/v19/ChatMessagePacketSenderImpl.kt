package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v19

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistries
import io.github.rothes.esu.bukkit.util.version.adapter.nms.NmsRegistryAccessHandler
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.ChatMessagePacketSender
import io.github.rothes.esu.lib.adventure.chat.ChatType
import io.github.rothes.esu.lib.adventure.chat.SignedMessage
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import org.bukkit.entity.Player

object ChatMessagePacketSenderImpl: ChatMessagePacketSender {

    private val REG = versioned<NmsRegistryAccessHandler>().getRegistryOrThrow(versioned<NmsRegistries>().chatType)
    private val SYSTEM = versioned<NmsRegistryAccessHandler>().getNullable(REG, net.minecraft.network.chat.ChatType.SYSTEM)!!
    private val SYSTEM_ID = versioned<NmsRegistryAccessHandler>().getId(REG, SYSTEM)

    override fun deleteMessage(player: Player, signature: SignedMessage.Signature) {
        // Maybe not supported
    }

    override fun sendSystemMessage(player: Player, message: Component) {
        player.handle.connection.send(ClientboundSystemChatPacket(message.toMinecraft(), SYSTEM_ID))
    }

    override fun sendMessage(player: Player, message: Component, boundChatType: ChatType.Bound) {
        player.handle.connection.send(ClientboundSystemChatPacket(message.toMinecraft(), SYSTEM_ID))
//        player.handle.sendChatMessage(PlayerChatMessage.unsigned(message.toMinecraft()), ChatSender(), net.minecraft.network.chat.ChatType.SYSTEM)
    }

}