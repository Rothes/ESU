package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v1

import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.TabListPacketSender
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundTabListPacket
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer
import org.bukkit.entity.Player

object TabListPacketSenderImpl: TabListPacketSender {

    private val HEADER_ACCESSOR = CraftPlayer::class.java.getDeclaredField("playerListHeader")
    private val FOOTER_ACCESSOR = CraftPlayer::class.java.getDeclaredField("playerListFooter")

    override fun sendPlayerListHeaderAndFooter(player: Player, header: Component, footer: Component) {
        player as CraftPlayer
        if (ServerInfo.isPaper) {
            HEADER_ACCESSOR[player] = header.server
            FOOTER_ACCESSOR[player] = footer.server
        } else {
            // TODO: Confirm fields on Spigot/CB
        }
        player.handle.connection.send(ClientboundTabListPacket(header.toMinecraft(), footer.toMinecraft()))
    }

}