package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.legacy
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.bukkit.util.version.Versioned
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.core.util.version.Version
import io.github.rothes.esu.lib.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

interface PlayerAdapter {

    fun getDisplayName(player: Player): Component
    fun setDisplayName(player: Player, name: Component?)

    companion object {

        val instance = if (ServerCompatibility.isPaper) Paper else CB

        private val playerChunkSentHandler by Versioned(PlayerChunkSentHandler::class.java)

        private val paper20 =
            ServerCompatibility.isPaper && ServerCompatibility.serverVersion >= Version.fromString("1.20")

        fun Player.chunkSent(chunkKey: Long): Boolean {
            return playerChunkSentHandler.isChunkSentNms(this, chunkKey)
        }

        fun Player.chunkSentBukkit(chunkKey: Long): Boolean {
            return playerChunkSentHandler.isChunkSentBukkit(this, chunkKey)
        }

        var Player.displayName_: Component
            get() = instance.getDisplayName(this)
            set(value) = instance.setDisplayName(this, value)

        val OfflinePlayer.connected: Boolean
            get() = if (paper20) isConnected else isOnline

        interface PlayerChunkSentHandler {

            fun isChunkSentNms(player: Player, chunkKey: Long): Boolean
            fun isChunkSentBukkit(player: Player, chunkKey: Long): Boolean

        }

    }

    private object CB: PlayerAdapter {

        override fun getDisplayName(player: Player): Component = player.displayName.legacy
        override fun setDisplayName(player: Player, name: Component?) = player.setDisplayName(name?.legacy)

    }

    private object Paper: PlayerAdapter {

        override fun getDisplayName(player: Player): Component = player.displayName().esu
        override fun setDisplayName(player: Player, name: Component?) = player.displayName(name?.server)

    }


}