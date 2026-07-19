package io.github.rothes.esu.bukkit.util.version.adapter.nms.network

import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.bukkit.util.version.adapter.adventure.AdventureConverter.toMinecraft
import io.github.rothes.esu.lib.adventure.resource.ResourcePackRequest
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import org.bukkit.entity.Player
import java.util.*
import net.minecraft.network.chat.Component as MinecraftComponent

interface ResourcePackPacketSender {

    fun sendResourcePacks(player: Player, request: ResourcePackRequest) {
        val prompt = request.prompt()?.toMinecraft()
        val packs = buildList(request.packs().size) {
            for (pack in request.packs()) {
                add(
                    pushPackPacket(
                        pack.id(), pack.uri().toASCIIString(), pack.hash(), request.required(),
                        if (size < request.packs().size) Optional.ofNullable(prompt) else Optional.empty()
                    )
                )
            }
        }
        BundlePacketSender.INSTANCE.send(player, packs)
    }
    fun removeResourcePacks(player: Player, id: UUID, vararg others: UUID) {
        BundlePacketSender.INSTANCE.send(player,
            buildList(others.size + 1) {
                add(popPackPacket(id))
                addAll(others.map { popPackPacket(it) })
            }
        )
    }
    fun clearResourcePacks(player: Player)

    fun pushPackPacket(id: UUID, url: String, hash: String, required: Boolean, prompt: Optional<MinecraftComponent>): Packet<in ClientGamePacketListener>
    fun popPackPacket(id: UUID): Packet<in ClientGamePacketListener>

    companion object {

        val INSTANCE = versioned<ResourcePackPacketSender>()

    }

}