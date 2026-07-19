package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v20_4

import io.github.rothes.esu.bukkit.util.version.adapter.nms.EntityHandleGetter.Companion.handle
import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.ResourcePackPacketSender
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket
import net.minecraft.network.protocol.game.ClientGamePacketListener
import org.bukkit.entity.Player
import java.util.*
import kotlin.jvm.optionals.getOrNull

object ResourcePackPacketSenderImpl: ResourcePackPacketSender {

    override fun clearResourcePacks(player: Player) {
        player.handle.connection.send(ClientboundResourcePackPopPacket(Optional.empty()))
    }

    override fun pushPackPacket(id: UUID, url: String, hash: String, required: Boolean, prompt: Optional<Component>): Packet<in ClientGamePacketListener> {
        return ClientboundResourcePackPushPacket(id, url, hash, required, prompt.getOrNull())
    }

    override fun popPackPacket(id: UUID): Packet<in ClientGamePacketListener> {
        return ClientboundResourcePackPopPacket(Optional.of(id))
    }

}