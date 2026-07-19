package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v20

import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.BundlePacketSender
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.entity.Player

object BundlePacketSenderImpl: BundlePacketSender {

    override fun send(player: Player, packets: Iterable<Packet<in ClientGamePacketListener>>) {
        @Suppress("UNCHECKED_CAST")
        packets as Iterable<Packet<ClientGamePacketListener>> // Dummy, this cast is not necessary on later Minecraft versions
        player.handle.connection.send(ClientboundBundlePacket(packets))
    }

}