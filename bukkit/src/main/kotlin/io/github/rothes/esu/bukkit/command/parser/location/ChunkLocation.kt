package io.github.rothes.esu.bukkit.command.parser.location

import org.bukkit.World

class ChunkLocation(
    val world: World?,
    val chunkX: Int,
    val chunkZ: Int,
) {

    constructor(world: World?, chunkX: Double, chunkZ: Double): this(world, chunkX.toInt(), chunkZ.toInt())

}