package io.github.rothes.esu.velocity.module.networkthrottle.channel

import com.velocitypowered.api.proxy.Player
import io.github.rothes.esu.lib.packetevents.protocol.packettype.PacketTypeCommon

data class PacketData(
    val player: Player?,
    val packetType: PacketTypeCommon,
    val uncompressedSize: Int,
    val compressedSize: Int,
)
