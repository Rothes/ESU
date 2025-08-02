package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_20_2

import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkSender
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.PlayerChunkSender
import net.minecraft.world.level.chunk.LevelChunk

class ChunkSenderImpl: ChunkSender {

    override fun sendChunk(player: ServerPlayer, level: ServerLevel, chunk: LevelChunk) {
        PlayerChunkSender.sendChunk(player.connection, level, chunk)
    }

}