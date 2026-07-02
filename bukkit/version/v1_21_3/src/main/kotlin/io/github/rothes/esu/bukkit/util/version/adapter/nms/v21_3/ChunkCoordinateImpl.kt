package io.github.rothes.esu.bukkit.util.version.adapter.nms.v21_3

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ChunkCoordinate
import net.minecraft.world.level.ChunkPos

object ChunkCoordinateImpl: ChunkCoordinate {

    override fun maxChunkCoordinate(): Int {
        return ChunkPos.MAX_COORDINATE_VALUE
    }

}