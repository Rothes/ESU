package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_17_1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.PalettedContainerReader
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

object PalettedContainerReaderImpl: PalettedContainerReader {

    private val storage = PalettedContainer::class.java.declaredFields.last { it.type == BitStorage::class.java }.usObjAccessor
    private val palette = PalettedContainer::class.java.declaredFields.last { it.type == Palette::class.java }.usObjAccessor


    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return storage[container] as BitStorage
    }

    override fun <T> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette[container] as Palette<T>
    }

}