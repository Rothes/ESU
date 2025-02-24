package io.github.rothes.esu.velocity.module.networkthrottle.channel

interface ChannelHandler {

    fun encode(packetData: PacketData) { }
    fun flush() { }

}