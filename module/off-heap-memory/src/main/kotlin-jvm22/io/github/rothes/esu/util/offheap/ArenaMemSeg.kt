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

package io.github.rothes.esu.util.offheap

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.experimental.or

class ArenaMemSeg(
    size: Long,
): MemSeg {

    companion object {
        private val NEED_INITIALIZE = java.lang.Boolean.getBoolean("jdk.internal.foreign.skipZeroMemory")
    }

    private val arena: Arena = Arena.ofShared()
    private val memorySegment: MemorySegment = arena.allocate(size)

    override val size: Long = memorySegment.byteSize()

    override fun get(offset: Long): Byte = memorySegment.get(ValueLayout.JAVA_BYTE, offset)
    override fun set(offset: Long, value: Byte) = memorySegment.set(ValueLayout.JAVA_BYTE, offset, value)

    override fun or(offset: Long, value: Byte) {
        set(offset, get(offset) or value)
    }

    override fun fill(offset: Long, length: Long, value: Byte) {
        memorySegment.asSlice(offset, length).fill(value)
    }

    override fun initialize() {
        if (NEED_INITIALIZE) memorySegment.fill(0)
    }

    override fun close() {
        arena.close()
    }
}