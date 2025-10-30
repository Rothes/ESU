package io.github.rothes.esu.velocity.module.networkthrottle.channel

import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.velocitypowered.api.proxy.Player

data class PacketData(
    val player: Player?,
    val packetType: PacketTypeCommon,
    val uncompressedSize: Int,
    val compressedSize: Int,
)
