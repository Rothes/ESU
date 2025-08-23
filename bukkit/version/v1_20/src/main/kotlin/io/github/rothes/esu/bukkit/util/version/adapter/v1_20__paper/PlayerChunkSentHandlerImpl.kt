package io.github.rothes.esu.bukkit.util.version.adapter.v1_20__paper

import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter
import io.papermc.paper.util.CoordinateUtils
import net.minecraft.server.level.ServerLevel
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer
import org.bukkit.entity.Player

class PlayerChunkSentHandlerImpl: PlayerAdapter.Companion.PlayerChunkSentHandler {

    override fun isChunkSentNms(player: Player, chunkKey: Long): Boolean {
        val nms = (player as CraftPlayer).handle
        return (nms.level() as ServerLevel).playerChunkLoader.isChunkSent(nms, CoordinateUtils.getChunkX(chunkKey), CoordinateUtils.getChunkZ(chunkKey))
    }

    override fun isChunkSentBukkit(player: Player, chunkKey: Long): Boolean {
        // Paper hasn't put this to bukkit api yet until 1.20.4 . Well confirmed.
        return isChunkSentNms(player, chunkKey)
    }

}