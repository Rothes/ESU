package io.github.rothes.esu.bukkit.util.version.adapter.nms.v18

import io.github.rothes.esu.bukkit.util.version.adapter.nms.PalettedContainerReader
import io.github.rothes.esu.core.util.UnsafeUtils
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

object PalettedContainerReaderImpl: PalettedContainerReader {

    private val data: UnsafeUtils.UnsafeObjAccessor
    private val storage: UnsafeUtils.UnsafeObjAccessor
    private val palette: UnsafeUtils.UnsafeObjAccessor

    init {
        val dataField = PalettedContainer::class.java.getDeclaredField("data")
        data = dataField.usObjAccessor
        storage = dataField.type.declaredFields.first { it.type == BitStorage::class.java }.usObjAccessor
        palette = dataField.type.declaredFields.first { it.type == Palette::class.java }.usObjAccessor
    }

    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return storage[data[container]] as BitStorage
    }

    override fun <T: Any> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette[data[container]] as Palette<T>
    }

}