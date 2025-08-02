package io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.v1_17_1

import io.github.rothes.esu.bukkit.module.networkthrottle.chunkdatathrottle.ChunkSender
import net.minecraft.network.protocol.Packet
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.chunk.LevelChunk

class ChunkSenderImpl: ChunkSender {

    override fun sendChunk(player: ServerPlayer, level: ServerLevel, chunk: LevelChunk) {
        level.chunkSource.chunkMap.updateChunkTracking(player, chunk.pos, Array<Packet<*>?>(2) { null }, false, true)
//        player.trackChunk(chunk.pos,
//            ClientboundLevelChunkPacket(chunk, chunk.level.chunkPacketBlockController.shouldModify(player, chunk)),
//            ClientboundLightUpdatePacket(chunk.pos, level.chunkSource.lightEngine, null, null, true)
//        )
    }

}