/*
 * This file is part of ESU - https://github.com/Rothes/ESU
 * Copyright (C) 2026 Rothes & contributors
 *
 * ESU is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * ESU is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ESU. If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.rothes.esu.bukkit.util.version.adapter

import io.github.rothes.esu.bukkit.util.ServerInfo
import io.github.rothes.esu.bukkit.util.version.VersionedInstance.versioned
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.lib.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

interface PlayerAdapter {

    fun getDisplayName(player: Player): Component
    fun setDisplayName(player: Player, name: Component?)

    companion object {

        val instance = if (ServerInfo.isPaper) Paper else CB

        private val playerChunkSentHandler = versioned<PlayerChunkSentHandler>()
        private val playerConnectedHandler = versioned<PlayerConnectedHandler>()

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
            get() = playerConnectedHandler.isPlayerConnected(this)

        interface PlayerChunkSentHandler {

            fun isChunkSentNms(player: Player, chunkKey: Long): Boolean
            fun isChunkSentBukkit(player: Player, chunkKey: Long): Boolean

        }

        interface PlayerConnectedHandler {
            fun isPlayerConnected(player: OfflinePlayer): Boolean
        }

    }

    @Suppress("DEPRECATION")
    private object CB: PlayerAdapter {

        override fun getDisplayName(player: Player): Component = player.displayName.legacy
        override fun setDisplayName(player: Player, name: Component?) = player.setDisplayName(name?.legacy)

    }

    private object Paper: PlayerAdapter {

        override fun getDisplayName(player: Player): Component = player.displayName().esu
        override fun setDisplayName(player: Player, name: Component?) = player.displayName(name?.server)

    }


}