/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.util.version.adapter.nms.v17_1

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

    override fun <T: Any> getPalette(container: PalettedContainer<T>): Palette<T> {
        @Suppress("UNCHECKED_CAST")
        return palette[container] as Palette<T>
    }

}