package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_21__paper

import io.github.rothes.esu.bukkit.util.version.adapter.nms.PalettedContainerReader
import io.github.rothes.esu.core.util.UnsafeUtils.usObjAccessor
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

object PalettedContainerReaderImplPaper: PalettedContainerReader {

    // 1.21, Paper made data field public
    private val dataType = PalettedContainer::class.java.getDeclaredField("data").type

    private val storage = dataType.declaredFields.first { it.type == BitStorage::class.java }.usObjAccessor
    private val palette = dataType.declaredFields.first { it.type == Palette::class.java }.usObjAccessor

    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return storage[container.data] as BitStorage
    }

    override fun <T> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette[container.data] as Palette<T>
    }

}