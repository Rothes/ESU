package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.legacy
import io.github.rothes.esu.bukkit.util.ServerCompatibility
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.core.util.version.Version
import net.kyori.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

interface PlayerAdapter {

    fun getDisplayName(player: Player): Component
    fun setDisplayName(player: Player, name: Component?)

    companion object {

        val instance = if (ServerCompatibility.paper) Paper else CB

        private val paper20 =
            ServerCompatibility.paper && ServerCompatibility.serverVersion >= Version.fromString("1.20")

        fun Player.chunkSent(chunkKey: Long): Boolean {
            return if (paper20) isChunkSent(chunkKey) else true
        }

        var Player.displayNameV: Component
            get() = instance.getDisplayName(this)
            set(value) = instance.setDisplayName(this, value)

        val OfflinePlayer.connected: Boolean
            get() = if (paper20) isConnected else true

    }

    private object CB: PlayerAdapter {

        override fun getDisplayName(player: Player): Component = player.displayName.legacy
        override fun setDisplayName(player: Player, name: Component?) = player.setDisplayName(name?.legacy)

    }

    private object Paper: PlayerAdapter {

        override fun getDisplayName(player: Player): Component = player.displayName()
        override fun setDisplayName(player: Player, name: Component?) = player.displayName(name)

    }


}