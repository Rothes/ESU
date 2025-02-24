package io.github.rothes.esu.velocity.module.networkthrottle.channel

interface DecoderChannelHandler {

    fun decode(packetData: PacketData) { }

}