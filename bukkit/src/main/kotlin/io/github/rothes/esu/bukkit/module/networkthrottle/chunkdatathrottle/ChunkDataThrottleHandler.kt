package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle

interface ChunkDataThrottleHandler {

    val counter: Counter

    fun enable()
    fun disable()
    fun reload()

    data class Counter(
        var minimalChunks: Long = 0,
        var resentChunks: Long = 0,
        var resentBlocks: Long = 0,
    )

}