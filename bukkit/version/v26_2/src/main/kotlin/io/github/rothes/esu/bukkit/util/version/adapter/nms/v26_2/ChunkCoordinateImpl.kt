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

package io.github.rothes.esu.bukkit.util.version.adapter.nms.v26_2

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ChunkCoordinate
import net.minecraft.world.level.chunk.status.ChunkPyramid

object ChunkCoordinateImpl: ChunkCoordinate {

    override fun maxChunkCoordinate(): Int {
        return ChunkPyramid.MAX_CHUNK_COORDINATE_VALUE
    }

}