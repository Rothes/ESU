package io.github.rothes.esu.util.offheap

import sun.misc.Unsafe

object MemoryAllocate {

    val UNSAFE: Unsafe

    init {
        val field = Unsafe::class.java.getDeclaredField("theUnsafe")
        field.isAccessible = true
        UNSAFE = field.get(null) as Unsafe
    }

    fun allocateBytes(bytes: Long): MemSeg {
        val address = UNSAFE.allocateMemory(bytes)
        return UnsafeMemSeg(address, bytes)
    }

}