package io.github.rothes.esu.bukkit.event.internal

import io.github.rothes.esu.bukkit.event.UserEmoteCommandEvent
import io.github.rothes.esu.bukkit.event.UserEmoteCommandEvent.Companion.EMOTE_COMMANDS
import io.github.rothes.esu.bukkit.event.UserReplyCommandEvent
import io.github.rothes.esu.bukkit.event.UserReplyCommandEvent.Companion.REPLY_COMMANDS
import io.github.rothes.esu.bukkit.event.UserWhisperCommandEvent
import io.github.rothes.esu.bukkit.event.UserWhisperCommandEvent.Companion.WHISPER_COMMANDS
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

internal object InternalListeners : Listener {

    private val emoteCommands = EMOTE_COMMANDS.split('|').toSet()
    private val whisperCommands = WHISPER_COMMANDS.split('|').toSet()
    private val replyCommands = REPLY_COMMANDS.split('|').toSet()

    init {
        register()
    }

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
            if (split.size >= 2 && UserEmoteCommandEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                val event = UserEmoteCommandEvent(
                    e.player,
                    split.drop(1).joinToString(separator = " "),
                    e.isCancelled,
                    priority,
                )
                event.callNested()
                e.isCancelled = event.isCancelled
            }
        } else if (whisperCommands.contains(command)) {
            if (split.size >= 3 && UserWhisperCommandEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                val event = UserWhisperCommandEvent(
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
            if (split.size >= 2 && UserReplyCommandEvent.getHandlerList().registeredListeners.isNotEmpty()) {
                val event = UserReplyCommandEvent(
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