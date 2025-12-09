package io.github.rothes.esu.util.offheap

interface MemSeg: AutoCloseable {

    val size: Long

    operator fun get(offset: Int) = get(offset.toLong())
    operator fun set(offset: Int, value: Byte) = set(offset.toLong(), value)
    operator fun get(offset: Long): Byte
    operator fun set(offset: Long, value: Byte)

    fun fill(offset: Long, length: Long, value: Byte)
    fun initialize()

}