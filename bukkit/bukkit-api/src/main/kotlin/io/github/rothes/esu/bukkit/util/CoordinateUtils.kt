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

package io.github.rothes.esu.bukkit.util

import org.bukkit.Chunk
import org.bukkit.block.Block

object CoordinateUtils {

    fun getChunkKey(x: Int, z: Int): Long {
        return (x.toLong() and 0xFFFFFFFFL) or (z.toLong() shl 32)
    }
    val Chunk.chunkKey_: Long
        get() = getChunkKey(x, z)

    val Long.chunkX: Int
        get() = toInt()
    val Long.chunkZ: Int
        get() = (this ushr 32).toInt()
    val Long.chunkPos
        get() = ChunkPos(chunkX, chunkZ)

    fun getBlockChunkKey(x: Int, y: Int, z: Int): Int {
        return (x and 0xf) or (z and 0xf shl 4) or (y shl 8)
    }
    val Block.blockChunkKey
        get() = getBlockChunkKey(x, y, z)

    val Int.blockChunkX: Int
        get() = this and 0xf
    val Int.blockChunkY: Int
        get() = this shr 8
    val Int.blockChunkZ: Int
        get() = this shr 4 and 0xf
    val Int.blockChunkPos
        get() = BlockChunkPos(blockChunkX, blockChunkY, blockChunkZ)

    /**
     * x, z: (-67108864 .. 67108863)
     * y: (-512 .. 511)
     */
    fun getBlockKey(x: Int, y: Int, z: Int): Long {
        return (x.toLong() and 0x7FFFFFFL) or ((z.toLong() and 0x7FFFFFFL) shl 27) or (y.toLong() shl 54)
    }
    val Block.blockKey_: Long
        get() = getBlockKey(x, y, z)

    val Long.blockX: Int
        get() = (this shl 37 shr 37).toInt() // Use shl+shr to keep sign
    val Long.blockY: Int
        get() = (this shr 54).toInt()
    val Long.blockZ: Int
        get() = (this shl 10 shr 37).toInt()
    val Long.blockPos: BlockPos
        get() = BlockPos(blockX, blockY, blockZ)

    data class ChunkPos(val x: Int, val z: Int)
    data class BlockChunkPos(val x: Int, val y: Int, val z: Int)
    data class BlockPos(val x: Int, val y: Int, val z: Int)

}