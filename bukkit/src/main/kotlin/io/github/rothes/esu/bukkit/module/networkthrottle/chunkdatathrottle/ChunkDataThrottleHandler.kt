package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle

import io.github.rothes.esu.core.module.CommonFeature

abstract class ChunkDataThrottleHandler<C, L>: CommonFeature<C, L>() {

    override val name: String = "ChunkHandler"

    val counter: Counter = Counter()

    data class Counter(
        var minimalChunks: Long = 0,
        var resentChunks: Long = 0,
        var resentBlocks: Long = 0,
    )

}