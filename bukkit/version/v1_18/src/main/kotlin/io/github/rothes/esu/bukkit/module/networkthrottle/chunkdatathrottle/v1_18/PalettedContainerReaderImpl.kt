package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_18

import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.PalettedContainerReader
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

class PalettedContainerReaderImpl: PalettedContainerReader {

    private val dataField = PalettedContainer::class.java.declaredFields.last { field ->
        field.type == PalettedContainer::class.java.declaredClasses.first { it.declaredFields.size == 3 }
    }
    private val data = dataField.getter(Any::class.java)
    private val storage = dataField.type.declaredFields.first { it.type == BitStorage::class.java }.getter(pType = Any::class.java)
    private val palette = dataField.type.declaredFields.first { it.type == Palette::class.java }.getter(pType = Any::class.java)

    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return storage.invokeExact(data.invokeExact(container)) as BitStorage
    }

    override fun <T> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette.invokeExact(data.invokeExact(container)) as Palette<T>
    }

}