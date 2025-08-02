package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.chunk.LevelChunk

interface ChunkSender {

    fun sendChunk(player: ServerPlayer, level: ServerLevel, chunk: LevelChunk)

}