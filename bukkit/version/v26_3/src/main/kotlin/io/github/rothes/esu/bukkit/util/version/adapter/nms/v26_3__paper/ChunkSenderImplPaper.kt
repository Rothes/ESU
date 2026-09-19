package io.github.rothes.esu.bukkit.util.version.adapter.nms.v26_3__paper

import io.github.rothes.esu.bukkit.util.version.adapter.nms.ChunkSender
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.chunk.LevelChunk

object ChunkSenderImplPaper: ChunkSender {

    override fun sendChunk(player: ServerPlayer, level: ServerLevel, chunk: LevelChunk) {
        val packet = ClientboundLevelChunkWithLightPacket(
            chunk,
            level.lightEngine,
            null,
            null,
        )
        packet.isReady = true // Skip paper anti-xray engine
        player.connection.send(packet)
    }

}