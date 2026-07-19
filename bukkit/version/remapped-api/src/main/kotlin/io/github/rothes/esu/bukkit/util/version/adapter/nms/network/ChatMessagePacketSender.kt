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