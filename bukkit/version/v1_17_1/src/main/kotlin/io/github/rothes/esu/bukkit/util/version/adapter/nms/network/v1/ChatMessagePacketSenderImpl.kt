/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

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