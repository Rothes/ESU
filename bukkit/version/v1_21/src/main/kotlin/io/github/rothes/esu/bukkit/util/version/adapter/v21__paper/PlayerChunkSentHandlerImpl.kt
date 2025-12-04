package io.github.rothes.esu.bukkit.util.version.adapter.v21__paper

import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.entity.Player

class PlayerChunkSentHandlerImpl: PlayerAdapter.Companion.PlayerChunkSentHandler {

    override fun isChunkSentNms(player: Player, chunkKey: Long): Boolean {
        val nms = (player as CraftPlayer).handle
        return nms.`moonrise$getChunkLoader`().sentChunksRaw.contains(chunkKey)
    }

    override fun isChunkSentBukkit(player: Player, chunkKey: Long): Boolean {
        return player.isChunkSent(chunkKey)
    }

}