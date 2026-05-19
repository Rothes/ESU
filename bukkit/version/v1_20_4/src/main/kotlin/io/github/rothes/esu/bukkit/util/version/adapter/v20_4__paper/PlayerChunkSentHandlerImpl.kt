/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.util.version.adapter.v20_4__paper

import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.entity.Player

// This API is added since 1.20.4 (Not a thing on Paper 1.20.3)
object PlayerChunkSentHandlerImpl: PlayerAdapter.Companion.PlayerChunkSentHandler {

    override fun isChunkSentNms(player: Player, chunkKey: Long): Boolean {
        val nms = (player as CraftPlayer).handle
        return nms.chunkLoader.sentChunksRaw.contains(chunkKey)
    }

    override fun isChunkSentBukkit(player: Player, chunkKey: Long): Boolean {
        return player.isChunkSent(chunkKey)
    }

}