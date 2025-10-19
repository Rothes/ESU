package io.github.rothes.esu.velocity.module.networkthrottle

import com.github.retrooper.packetevents.protocol.PacketSide
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.wrapper.PacketWrapper

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