package io.github.rothes.esu.bukkit.util.version.adapter.nms

import io.github.rothes.esu.bukkit.util.scheduler.Scheduler.onTick
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.chunk.LevelChunk

interface ChunkSender {

    fun sendChunkSafely(player: ServerPlayer, level: ServerLevel, chunk: LevelChunk) {
        try {
            sendChunk(player, level, chunk)
        } catch (_: Exception) {
            // If block entities in chunk is being modified, this throws error.
            player.bukkitEntity.onTick {
                sendChunk(player, level, chunk)
            }
        }
    }

    fun sendChunk(player: ServerPlayer, level: ServerLevel, chunk: LevelChunk)

}