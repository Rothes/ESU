package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v17

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.TitlePacketSender
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.network.protocol.game.*
import org.bukkit.entity.Player

object TitlePacketSenderImpl: TitlePacketSender {

    override fun sendActionBar(player: Player, message: Component) {
        player.handle.connection.send(ClientboundSetActionBarTextPacket(message.toMinecraft()))
    }

    override fun sendTitlesAnimation(player: Player, fadeIn: Int, stay: Int, fadeOut: Int) {
        player.handle.connection.send(ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut))
    }

    override fun sendTitle(player: Player, message: Component) {
        player.handle.connection.send(ClientboundSetTitleTextPacket(message.toMinecraft()))
    }

    override fun sendSubtitle(player: Player, message: Component) {
        player.handle.connection.send(ClientboundSetSubtitleTextPacket(message.toMinecraft()))
    }

    override fun clearTitle(player: Player) {
        player.handle.connection.send(ClientboundClearTitlesPacket(false))
    }

    override fun resetTitle(player: Player) {
        player.handle.connection.send(ClientboundClearTitlesPacket(true))
    }

}