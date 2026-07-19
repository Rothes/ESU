package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.BundlePacketSender
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import org.bukkit.entity.Player

object BundlePacketSenderImpl: BundlePacketSender {

    override fun send(player: Player, packets: Iterable<Packet<in ClientGamePacketListener>>) {
        val connection = player.handle.connection ?: return
        for (packet in packets) {
            connection.send(packet)
        }
    }

}