package io.github.rothes.esu.util.offheap

class UnsafeMemSeg(
    private val address: Long,
    override val size: Long,
): MemSeg {

    companion object { private val UNSAFE = MemoryAllocate.UNSAFE }

    override fun get(offset: Long): Byte = UNSAFE.getByte(address + offset)
    override fun set(offset: Long, value: Byte) = UNSAFE.putByte(address + offset, value)

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