package io.github.rothes.esu.velocity.module.networkthrottle.channel

interface ChannelHandler {

    fun handle(packetData: PacketData)

}