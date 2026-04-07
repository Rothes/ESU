package io.github.rothes.esu.bukkit.util.version.adapter.v20_2__paper

import io.github.rothes.esu.bukkit.util.version.adapter.PlayerAdapter
import org.bukkit.OfflinePlayer

object PlayerConnectedHandlerImpl: PlayerAdapter.Companion.PlayerConnectedHandler {

    override fun isPlayerConnected(player: OfflinePlayer): Boolean {
        return player.isConnected
    }
}