package io.github.rothes.esu.velocity.module.networkthrottle.channel

interface EncoderChannelHandler {

    fun encode(packetData: PacketData) { }

    fun flush(totalBufferSize: Int) {
        flush()
    }

    fun flush() { }

}