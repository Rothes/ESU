package io.github.rothes.esu.util.offheap

import java.lang.Boolean
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.Byte
import kotlin.Long

class ArenaMemSeg(
    size: Long,
): MemSeg {

    companion object {
        private val NEED_INITIALIZE = Boolean.getBoolean("jdk.internal.foreign.skipZeroMemory")
    }

    private val arena: Arena = Arena.ofShared()
    private val memorySegment: MemorySegment = arena.allocate(size)

    override val size: Long = memorySegment.byteSize()

    override fun get(offset: Long): Byte = memorySegment.get(ValueLayout.JAVA_BYTE, offset)
    override fun set(offset: Long, value: Byte) = memorySegment.set(ValueLayout.JAVA_BYTE, offset, value)

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