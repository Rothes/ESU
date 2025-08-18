package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_21__paper

import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.PalettedContainerReader
import io.github.rothes.esu.core.util.ReflectionUtils.getter
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

class PalettedContainerReaderImplPaper: PalettedContainerReader {

    // 1.21, Paper made data field public
    private val dataType = PalettedContainer::class.java.getDeclaredField("data").type
    private val storage = dataType.declaredFields.first { it.type == BitStorage::class.java }.getter
    private val palette = dataType.declaredFields.first { it.type == Palette::class.java }.getter

    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return storage.invokeExact(container.data) as BitStorage
    }

    override fun <T> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette.invokeExact(container.data) as Palette<T>
    }

}