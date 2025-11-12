package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_21_3__paper

import io.github.rothes.esu.bukkit.util.version.adapter.nms.PalettedContainerReader
import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

object PalettedContainerReaderImplPaper: PalettedContainerReader {

    // 1.21.3, Paper added FastPaletteData

    override fun getStorage(container: PalettedContainer<*>): BitStorage {
        return container.data.storage()
    }

    override fun <T> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return container.data.palette()
    }

}