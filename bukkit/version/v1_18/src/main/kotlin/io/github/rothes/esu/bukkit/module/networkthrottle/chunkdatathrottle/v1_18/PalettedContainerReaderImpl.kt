package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_18

import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.PalettedContainerReader
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer
import java.lang.reflect.Field

class PalettedContainerReaderImpl: PalettedContainerReader {

    private val data: Field = PalettedContainer::class.java.declaredFields.last { field ->
        field.type == PalettedContainer::class.java.declaredClasses.first { it.declaredFields.size == 3 }
    }.also { it.isAccessible = true }
    private val storage = data.type.declaredFields.first { it.type == BitStorage::class.java }.also { it.isAccessible = true }
    private val palette = data.type.declaredFields.first { it.type == Palette::class.java }.also { it.isAccessible = true }

    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return storage[data[container]] as BitStorage
    }

    override fun <T> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette[data[container]] as Palette<T>
    }

}