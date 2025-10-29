package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.util.BitStorage
import net.minecraft.world.level.chunk.Palette
import net.minecraft.world.level.chunk.PalettedContainer

interface PalettedContainerReader {

    fun getStorage(container: PalettedContainer<*>): BitStorage
    fun <T> getPalette(container: PalettedContainer<T>): Palette<T>

}