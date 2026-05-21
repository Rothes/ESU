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

import kotlin.experimental.or

class UnsafeMemSeg(
    private val address: Long,
    override val size: Long,
): MemSeg {

    companion object { private val UNSAFE = MemoryAllocate.UNSAFE }

    override fun get(offset: Int): Byte = UNSAFE.getByte(address + offset)
    override fun set(offset: Int, value: Byte) = UNSAFE.putByte(address + offset, value)
    override fun get(offset: Long): Byte = UNSAFE.getByte(address + offset)
    override fun set(offset: Long, value: Byte) = UNSAFE.putByte(address + offset, value)

    override fun or(offset: Long, value: Byte) {
        val addr = address + offset
        UNSAFE.putByte(addr, UNSAFE.getByte(addr) or value)
    }

    override fun fill(offset: Long, length: Long, value: Byte) {
        UNSAFE.setMemory(address + offset, length, value)
    }

    override fun initialize() {
        UNSAFE.setMemory(address, size, 0)
    }

    override fun close() {
        UNSAFE.freeMemory(address)
    }

}