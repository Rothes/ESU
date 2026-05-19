/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.util.version.adapter.nms.v21__paper

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

    override fun <T: Any> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette[container.data] as Palette<T>
    }

}