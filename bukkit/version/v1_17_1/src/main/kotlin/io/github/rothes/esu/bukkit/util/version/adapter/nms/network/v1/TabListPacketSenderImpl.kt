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

import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.TabListPacketSender
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundTabListPacket
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer
import org.bukkit.entity.Player

object TabListPacketSenderImpl: TabListPacketSender {

    private val HEADER_ACCESSOR = CraftPlayer::class.java.getDeclaredField("playerListHeader")
    private val FOOTER_ACCESSOR = CraftPlayer::class.java.getDeclaredField("playerListFooter")

    override fun sendPlayerListHeaderAndFooter(player: Player, header: Component, footer: Component) {
        if (ServerInfo.isPaper) {
            HEADER_ACCESSOR[player] = header.server
            FOOTER_ACCESSOR[player] = footer.server
        } else {
            // TODO: Confirm fields on Spigot/CB
        }
        player.handle.connection.send(ClientboundTabListPacket(header.toMinecraft(), footer.toMinecraft()))
    }

}