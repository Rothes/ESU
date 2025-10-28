package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle

interface ChunkDataThrottleHandler {

    val counter: Counter

    fun onEnable()
    fun onReload()
    fun onTerminate()

    data class Counter(
        var minimalChunks: Long = 0,
        var resentChunks: Long = 0,
        var resentBlocks: Long = 0,
    )

}