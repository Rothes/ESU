package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle

import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

interface PalettedContainerReader {

//    fun getState(section: LevelChunkSection): PalettedContainer<BlockState>

    fun getStorage(container: PalettedContainer<*>): BitStorage
    fun <T> getPalette(container: PalettedContainer<T>): Palette<T>

}