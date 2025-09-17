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

    data class ChunkPos(val x: Int, val z: Int)
    data class BlockChunkPos(val x: Int, val y: Int, val z: Int)

}