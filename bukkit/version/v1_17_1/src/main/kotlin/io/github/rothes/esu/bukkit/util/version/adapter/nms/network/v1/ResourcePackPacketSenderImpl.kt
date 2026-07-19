package io.github.rothes.esu.bukkit.util.version.adapter.nms.network.v1

import io.github.rothes.esu.bukkit.util.version.adapter.nms.network.ResourcePackPacketSender
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket
import org.bukkit.entity.Player
import java.util.*
import kotlin.jvm.optionals.getOrNull

object ResourcePackPacketSenderImpl: ResourcePackPacketSender {

    override fun clearResourcePacks(player: Player) {
        // Not supported on Minecraft version
    }

    override fun pushPackPacket(id: UUID, url: String, hash: String, required: Boolean, prompt: Optional<Component>): Packet<in ClientGamePacketListener> {
        return ClientboundResourcePackPacket(url, hash, required, prompt.getOrNull())
    }

    override fun popPackPacket(id: UUID): Packet<in ClientGamePacketListener> {
        error("Not supported on Minecraft version")
    }

}