package io.github.rothes.esu.velocity.module.networkthrottle

import io.github.rothes.esu.lib.packetevents.protocol.PacketSide
import io.github.rothes.esu.lib.packetevents.protocol.packettype.PacketTypeCommon
import io.github.rothes.esu.lib.packetevents.protocol.player.ClientVersion
import io.github.rothes.esu.lib.packetevents.wrapper.PacketWrapper

object UnknownPacketType: PacketTypeCommon {

    override fun getName(): String {
        return "unknown"
    }

    override fun getId(version: ClientVersion?): Int {
        return -1
    }

    override fun getSide(): PacketSide? {
        return null
    }

    override fun getWrapperClass(): Class<out PacketWrapper<*>?>? {
        return null
    }

}