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