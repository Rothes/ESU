package io.github.rothes.esu.velocity.module.networkthrottle.channel

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.velocitypowered.api.proxy.Player
import io.netty.buffer.ByteBuf

data class PacketData(
    val player: Player?,
    val packetType: PacketTypeCommon,
    val buf: ByteBuf,
    val uncompressedSize: Int,
    val compressedSize: Int,
)
