package io.github.rothes.esu.util.offheap

object MemoryAllocateDisabled { // Disable this while Unsafe is much faster than this...

    fun allocateBytes(bytes: Long): MemSeg {
        return ArenaMemSeg(bytes)
    }

}