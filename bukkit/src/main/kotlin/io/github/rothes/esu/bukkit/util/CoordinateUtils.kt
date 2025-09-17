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

    fun getBlockChunkKey(x: Int, y: Int, z: Int, minHeight: Int): Int {
        return (x and 0xf) or (z and 0xf shl 4) or (y - minHeight shl 8)
    }
    val Block.blockChunkKey
        get() = getBlockChunkKey(x, y, z, world.minHeight)

    fun Int.chunkBlockPos(minHeight: Int): ChunkBlockPos {
        return ChunkBlockPos(this and 0xf, (this shr 8) + minHeight, this shr 4 and 0xf)
    }

    data class ChunkPos(val x: Int, val z: Int)
    data class ChunkBlockPos(val x: Int, val y: Int, val z: Int)

}