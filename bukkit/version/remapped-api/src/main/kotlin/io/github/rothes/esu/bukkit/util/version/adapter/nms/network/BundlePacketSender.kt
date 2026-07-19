package io.github.rothes.esu.bukkit.util.version.adapter.nms.network

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import org.bukkit.entity.Player

interface BundlePacketSender {

    fun send(player: Player, packets: Iterable<Packet<in ClientGamePacketListener>>)

    companion object {

        val INSTANCE = versioned<BundlePacketSender>()

    }

}