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

package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v20_4

import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.ResourcePackPacketSender
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.network.protocol.game.ClientGamePacketListener
import org.bukkit.entity.Player
import java.util.*
import kotlin.jvm.optionals.getOrNull

object ResourcePackPacketSenderImpl: ResourcePackPacketSender {

    override fun clearResourcePacks(player: Player) {
        player.handle.connection.send(ClientboundResourcePackPopPacket(Optional.empty()))
    }

    override fun pushPackPacket(id: UUID, url: String, hash: String, required: Boolean, prompt: Optional<Component>): Packet<in ClientGamePacketListener> {
        return ClientboundResourcePackPushPacket(id, url, hash, required, prompt.getOrNull())
    }

    override fun popPackPacket(id: UUID): Packet<in ClientGamePacketListener> {
        return ClientboundResourcePackPopPacket(Optional.of(id))
    }

}