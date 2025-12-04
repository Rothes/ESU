package io.github.rothes.esu.bukkit.util.version.adapter.nms.v20_2__paper

import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.syncTick
import io.github.rothes.esu.bukkit.util.version.adapter.nms.ChunkSender
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.chunk.LevelChunk

class ChunkSenderImplPaper: ChunkSender {

    override fun sendChunk(player: ServerPlayer, level: ServerLevel, chunk: LevelChunk) {
        player.bukkitEntity.syncTick {
            player.connection.send(
                ClientboundLevelChunkWithLightPacket(
                    chunk,
                    level.lightEngine,
                    null,
                    null,
                     false
                )
            )
        }
    }

}