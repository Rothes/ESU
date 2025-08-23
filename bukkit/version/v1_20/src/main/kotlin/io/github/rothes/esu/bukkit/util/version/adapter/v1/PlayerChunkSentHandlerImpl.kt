package io.github.rothes.esu.bukkit.util.version.adapter.v1

import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter
import org.bukkit.entity.Player

class PlayerChunkSentHandlerImpl: PlayerAdapter.Companion.PlayerChunkSentHandler {

    // Fallback of versions before paper 1.20, which doesn't contain this feature.

    override fun isChunkSentNms(player: Player, chunkKey: Long): Boolean {
        return true
    }

    override fun isChunkSentBukkit(player: Player, chunkKey: Long): Boolean {
        return true
    }

}