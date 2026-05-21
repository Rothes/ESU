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

package io.github.rothes.esu.bukkit.module.chatantispam.message

import io.github.rothes.esu.bukkit.module.ChatAntiSpamModule
import io.github.rothes.esu.bukkit.module.chatantispam.message.meta.MessageContext
import io.github.rothes.esu.bukkit.module.chatantispam.user.SpamData
import io.github.rothes.esu.bukkit.user
import org.bukkit.entity.Player

data class MessageRequest(
    val player: Player,
    val context: MessageContext,
    val spamCheck: ChatAntiSpamModule.ModuleConfig.SpamCheck,
    val spamData: SpamData,
    val sendTime: Long,
    val afkTime: Long,
    val rawMessage: String,
    var message: String = rawMessage
) {
    val user by lazy { player.user }
}
