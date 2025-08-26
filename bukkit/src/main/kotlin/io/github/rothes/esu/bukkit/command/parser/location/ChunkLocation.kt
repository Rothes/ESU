package io.github.rothes.esu.bukkit.command.parser.location

import org.bukkit.World
import kotlin.math.round

class ChunkLocation(
    val world: World?,
    val chunkX: Int,
    val chunkZ: Int,
) {

    constructor(world: World?, chunkX: Double, chunkZ: Double): this(world, round(chunkX).toInt(), round(chunkZ).toInt())

}