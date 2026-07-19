package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v1

import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.TitlePacketSender
import io.github.rothes.esu.lib.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundChatPacket
import org.bukkit.entity.Player
import net.minecraft.network.chat.ChatType as MinecraftChatType

object TitlePacketSenderImpl: TitlePacketSender {

    override fun sendActionBar(player: Player, message: Component) {
        // TODO: On 1.11-1.16.5, client does not render colors, should use TitlePacket with actionbar action instead, but we cannot impl with paperweight
        player.handle.connection.send(ClientboundChatPacket(message.toMinecraft(), MinecraftChatType.GAME_INFO, null))
    }

    // TODO: Titles impl for 1.16.5 and below required
    override fun sendTitlesAnimation(player: Player, fadeIn: Int, stay: Int, fadeOut: Int) {
        TODO("Not yet implemented")
    }

    override fun sendTitle(player: Player, message: Component) {
        TODO("Not yet implemented")
    }

    override fun sendSubtitle(player: Player, message: Component) {
        TODO("Not yet implemented")
    }

    override fun clearTitle(player: Player) {
        TODO("Not yet implemented")
    }

    override fun resetTitle(player: Player) {
        TODO("Not yet implemented")
    }

}