package io.github.rothes.esu.bukkit.util.version.adapter.v1_20_4__paper

import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer
import org.bukkit.entity.Player

class PlayerChunkSentHandlerImpl: PlayerAdapter.Companion.PlayerChunkSentHandler {

    override fun isChunkSentNms(player: Player, chunkKey: Long): Boolean {
        val nms = (player as CraftPlayer).handle
        return nms.chunkLoader.sentChunksRaw.contains(chunkKey)
    }

    override fun isChunkSentBukkit(player: Player, chunkKey: Long): Boolean {
        return player.isChunkSent(chunkKey)
    }

}