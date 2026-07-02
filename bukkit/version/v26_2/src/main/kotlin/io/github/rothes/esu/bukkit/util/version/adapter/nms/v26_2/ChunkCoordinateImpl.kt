package io.github.rothes.esu.bukkit.util.version.adapter.nms.v26_2

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ChunkCoordinate
import net.minecraft.world.level.chunk.status.ChunkPyramid

object ChunkCoordinateImpl: ChunkCoordinate {

    override fun maxChunkCoordinate(): Int {
        return ChunkPyramid.MAX_CHUNK_COORDINATE_VALUE
    }

}