package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ChunkCoordinate

object ChunkCoordinateImpl: ChunkCoordinate {

    override fun maxChunkCoordinate(): Int {
        return Int.MAX_VALUE // TODO: Max value limit unsure. Use Int_MAX_VALUE for now.
    }

}