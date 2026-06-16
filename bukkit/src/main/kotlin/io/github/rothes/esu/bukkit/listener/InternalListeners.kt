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

package io.github.rothes.esu.bukkit.listener

import io.github.rothes.esu.bukkit.UpdateCheckerMan
import io.github.rothes.esu.bukkit.event.*
import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.util.extension.register
import io.github.rothes.esu.bukkit.util.inventory.InventoryUtils.esuHolder
import io.github.rothes.esu.core.storage.StorageManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

internal object InternalListeners : Listener {

    private val emoteCommands = RawUserEmoteEvent.EMOTE_COMMANDS.split('|').toSet()
    private val whisperCommands = RawUserWhisperEvent.WHISPER_COMMANDS.split('|').toSet()
    private val replyCommands = RawUserReplyEvent.REPLY_COMMANDS.split('|').toSet()

    init {
        register()

        // Init event listeners
        UserLoginEvent.Companion
    }

    /* User data loading */

    @EventHandler(priority = EventPriority.MONITOR)
    fun onLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult == AsyncPlayerPreLoginEvent.Result.ALLOWED)
            BukkitUserManager[event.uniqueId]
        else
            BukkitUserManager.unload(event.uniqueId)
    }
    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        val user = BukkitUserManager[event.player]
        UpdateCheckerMan.onJoin(user)
    }
    @EventHandler(priority = EventPriority.MONITOR)
    fun onQuit(event: PlayerQuitEvent) {
        BukkitUserManager.getCache(event.player.uniqueId)?.let {
            StorageManager.updateUserAsync(it)
            BukkitUserManager.unload(it)
        }
    }

    /* Rich message events */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerDeath(event: PlayerDeathEvent) {
        RichPlayerDeathEvent(event).callEvent()
    }

    /* EsuInvHolder processing */

    @EventHandler(priority = EventPriority.LOWEST)
    fun onClick(e: InventoryClickEvent) {
        val holder = e.inventory.esuHolder ?: return
        holder.handleClick(e)
    }
    @EventHandler(priority = EventPriority.LOWEST)
    fun onDrag(e: InventoryDragEvent) {
        val holder = e.inventory.esuHolder ?: return
        holder.handleDrag(e)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onClose(e: InventoryCloseEvent) {
        val holder = e.inventory.esuHolder ?: return
        holder.onClose()
    }

    /* Esu nested events processing */

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChatCommand0(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.LOWEST)
    }
    @EventHandler(priority = EventPriority.LOW)
    fun onChatCommand1(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.LOW)
    }
    @EventHandler(priority = EventPriority.NORMAL)
    fun onChatCommand2(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.NORMAL)
    }
    @EventHandler(priority = EventPriority.HIGH)
    fun onChatCommand3(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.HIGH)
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onChatCommand4(e: PlayerCommandPreprocessEvent) {
        onChatCommand(e, EventPriority.HIGHEST)
    }

    private fun onChatCommand(e: PlayerCommandPreprocessEvent, priority: EventPriority) {
        val message = e.message
        val split = message.split(' ', limit = 3)
        val command = split[0].substring(1).split(':').last().lowercase()
        if (emoteCommands.contains(command)) {
            if (split.size >= 2 && RawUserEmoteEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                val event = RawUserEmoteEvent(
                    e.player,
                    split.drop(1).joinToString(separator = " "),
                    e.isCancelled,
                    priority,
                )
                event.callNested()
                e.isCancelled = event.isCancelled
            }
        } else if (whisperCommands.contains(command)) {
            if (split.size >= 3 && RawUserWhisperEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                val event = RawUserWhisperEvent(
                    e.player,
                    split[1],
                    split[2],
                    e.isCancelled,
                    priority,
                )
                event.callNested()
                e.isCancelled = event.isCancelled
            }
        } else if (replyCommands.contains(command)) {
            if (split.size >= 2 && RawUserReplyEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                val event = RawUserReplyEvent(
                    e.player,
                    split.drop(1).joinToString(separator = " "),
                    e.isCancelled,
                    priority,
                )
                event.callNested()
                e.isCancelled = event.isCancelled
            }
        }
    }

}