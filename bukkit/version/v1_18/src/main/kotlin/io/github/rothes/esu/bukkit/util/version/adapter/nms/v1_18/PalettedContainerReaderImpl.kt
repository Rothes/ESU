package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_18

import io.github.rothes.esu.bukkit.util.version.adapter.nms.PalettedContainerReader
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

object PalettedContainerReaderImpl: PalettedContainerReader {

    private val dataField = PalettedContainer::class.java.declaredFields.last { field ->
        field.type == PalettedContainer::class.java.declaredClasses.first { it.declaredFields.size == 3 }
    }
    private val data = dataField.usObjAccessor
    private val storage = dataField.type.declaredFields.first { it.type == BitStorage::class.java }.usObjAccessor
    private val palette = dataField.type.declaredFields.first { it.type == Palette::class.java }.usObjAccessor

    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return storage[data[container]] as BitStorage
    }

    override fun <T> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette[data[container]] as Palette<T>
    }

}