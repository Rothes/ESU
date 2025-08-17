package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_17_1

import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.PalettedContainerReader
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

class PalettedContainerReaderImpl: PalettedContainerReader {

    private val storage = PalettedContainer::class.java.declaredFields.last { it.type == BitStorage::class.java }.getter
    private val palette = PalettedContainer::class.java.declaredFields.last { it.type == Palette::class.java }.getter


    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return storage.invokeExact(container) as BitStorage
    }

    override fun <T> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette.invokeExact(container) as Palette<T>
    }

}