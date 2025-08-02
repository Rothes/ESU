package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_17_1

import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.PalettedContainerReader
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

class PalettedContainerReaderImpl: PalettedContainerReader {

    private val storage = PalettedContainer::class.java.declaredFields.last { it.type == BitStorage::class.java }.also { it.isAccessible = true }
    private val palette = PalettedContainer::class.java.declaredFields.last { it.type == Palette::class.java }.also { it.isAccessible = true }

//    override fun getState(section: LevelChunkSection): PalettedContainer<BlockState> {
//        return section.states
//    }

    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return storage[container] as BitStorage
    }

    override fun <T> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette[container] as Palette<T>
    }

}