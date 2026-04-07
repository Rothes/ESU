package io.github.rothes.esu.bukkit.util.version.adapter.v1

import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter
import org.bukkit.OfflinePlayer
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer

object PlayerConnectedHandlerImpl: PlayerAdapter.Companion.PlayerConnectedHandler {

    override fun isPlayerConnected(player: OfflinePlayer): Boolean {
        if (player is CraftPlayer) {
            return !player.handle.hasDisconnected()
        }
        // Offline player instance
        return false
    }
}