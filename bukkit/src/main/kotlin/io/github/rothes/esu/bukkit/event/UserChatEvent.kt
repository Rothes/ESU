package io.github.rothes.esu.bukkit.event

import io.github.rothes.esu.bukkit.legacy
import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.lib.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.*

class UserChatEvent(
    async: Boolean,
    val player: Player,
    message: Component,
    private var cancelled: Boolean,
): Event(async), Cancellable {

    private var changed: Boolean = false

    val user: PlayerUser by lazy {
        player.user
    }

    var message: Component = message
        set(value) {
            changed = true
            field = value
        }

    override fun isCancelled(): Boolean {
        return cancelled
    }
    override fun setCancelled(cancel: Boolean) {
        this.cancelled = cancel
    }

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {
        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers

        init {
            val listener = try {
                io.papermc.paper.event.player.AsyncChatEvent::class.java.toString()
                object : Listener {
                    @EventHandler(priority = EventPriority.HIGH)
                    fun onChat(event: io.papermc.paper.event.player.AsyncChatEvent) {
                        val esuEvent = UserChatEvent(event.isAsynchronous, event.player, event.message().esu, event.isCancelled)
                        Bukkit.getPluginManager().callEvent(esuEvent)

                        event.isCancelled = esuEvent.isCancelled
                        if (esuEvent.changed)
                            event.message(esuEvent.message.server)
                    }
                }
            } catch (_: NoClassDefFoundError) {
                object : Listener {
                    @Suppress("DEPRECATION")
                    @EventHandler(priority = EventPriority.HIGH)
                    fun onChat(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
                        val esuEvent = UserChatEvent(event.isAsynchronous, event.player, event.message.legacy, event.isCancelled)
                        Bukkit.getPluginManager().callEvent(esuEvent)

                        event.isCancelled = esuEvent.isCancelled
                        if (esuEvent.changed)
                            event.message = esuEvent.message.legacy
                    }
                }
            }
            listener.register()
        }
    }
}