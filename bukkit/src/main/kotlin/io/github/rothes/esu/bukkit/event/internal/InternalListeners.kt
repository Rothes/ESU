package io.github.rothes.esu.bukkit.event.internal

import io.github.rothes.esu.bukkit.UpdateCheckerMan
import io.github.rothes.esu.bukkit.event.RawUserEmoteEvent
import io.github.rothes.esu.bukkit.event.RawUserEmoteEvent.Companion.EMOTE_COMMANDS
import io.github.rothes.esu.bukkit.event.RawUserReplyEvent
import io.github.rothes.esu.bukkit.event.RawUserReplyEvent.Companion.REPLY_COMMANDS
import io.github.rothes.esu.bukkit.event.RawUserWhisperEvent
import io.github.rothes.esu.bukkit.event.RawUserWhisperEvent.Companion.WHISPER_COMMANDS
import io.github.rothes.esu.bukkit.inventory.EsuInvHolder
import io.github.rothes.esu.bukkit.user.BukkitUserManager
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.core.storage.StorageManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

internal object InternalListeners : Listener {

    private val emoteCommands = EMOTE_COMMANDS.split('|').toSet()
    private val whisperCommands = WHISPER_COMMANDS.split('|').toSet()
    private val replyCommands = REPLY_COMMANDS.split('|').toSet()

    init {
        register()
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
    fun onLogin(event: PlayerJoinEvent) {
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

    /* EsuInvHolder processing */

    @EventHandler(priority = EventPriority.LOWEST)
    fun onClick(e: InventoryClickEvent) {
        val holder = e.inventory.holder
        if (holder is EsuInvHolder<*>) {
            holder.handleClick(e)
        }
    }
    @EventHandler(priority = EventPriority.LOWEST)
    fun onClick(e: InventoryDragEvent) {
        val holder = e.inventory.holder
        if (holder is EsuInvHolder<*>) {
            holder.handleDrag(e)
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onClick(e: InventoryCloseEvent) {
        val holder = e.inventory.holder
        if (holder is EsuInvHolder<*>) {
            holder.onClose()
        }
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