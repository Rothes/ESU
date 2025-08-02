package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle

interface ChunkDataThrottleHandler {

    val counter: Counter

    fun enable()
    fun disable()

    companion object {
        // The block B is in center. if Y_MINUS, block in B's bottom is occluding(i.e. blocking) .
        const val X_PLUS    = 0b00001.toByte()
        const val X_MINUS   = 0b00010.toByte()
        const val Z_PLUS    = 0b00100.toByte()
        const val Z_MINUS   = 0b01000.toByte()
        const val Y_MINUS   = 0b10000.toByte()
        const val INVISIBLE = 0b11111.toByte()
    }

    data class Counter(
        var minimalChunks: Long = 0,
        var resentChunks: Long = 0,
        var resentBlocks: Long = 0,
    )

}