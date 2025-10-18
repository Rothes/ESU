package io.github.rothes.esu.bukkit.event

import io.github.rothes.esu.bukkit.legacy
import io.github.rothes.esu.bukkit.util.extension.ListenerExt.register
import io.github.rothes.esu.core.util.AdventureConverter.esu
import io.github.rothes.esu.core.util.AdventureConverter.server
import io.github.rothes.esu.core.util.ComponentUtils.legacy
import io.github.rothes.esu.lib.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.*
import org.bukkit.event.player.PlayerEvent

class UserChatEvent(
    async: Boolean,
    player: Player,
    message: Component,
    override var cancelledKt: Boolean,
    override val parentPriority: EventPriority
): EsuUserEvent(player, async), CancellableKt, Nested {

    private var changed: Boolean = false

    var message: Component = message
        set(value) {
            changed = true
            field = value
        }

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {

        private val handlers = HandlerList()
        @JvmStatic
        fun getHandlerList(): HandlerList = handlers

        init {
            fun <T> callChatEvent(
                event: T,
                message: Component,
                priority: EventPriority
            ): UserChatEvent where T : PlayerEvent, T : Cancellable {
                val esuEvent =
                    UserChatEvent(event.isAsynchronous, event.player, message, event.isCancelled, priority)
                esuEvent.callNested()

                event.isCancelled = esuEvent.cancelledKt
                return esuEvent
            }

            try {
                io.papermc.paper.event.player.AsyncChatEvent::class.java.toString()
                object : Listener {
                    fun callPaper(event: io.papermc.paper.event.player.AsyncChatEvent, priority: EventPriority) {
                        val esu = callChatEvent(event, event.message().esu, priority)
                        if (esu.changed) event.message(esu.message.server)
                    }

                    @EventHandler(priority = EventPriority.LOWEST)
                    fun onChat0(event: io.papermc.paper.event.player.AsyncChatEvent) {
                        callPaper(event, EventPriority.LOWEST)
                    }
                    @EventHandler(priority = EventPriority.LOW)
                    fun onChat1(event: io.papermc.paper.event.player.AsyncChatEvent) {
                        callPaper(event, EventPriority.LOW)
                    }
                    @EventHandler(priority = EventPriority.NORMAL)
                    fun onChat2(event: io.papermc.paper.event.player.AsyncChatEvent) {
                        callPaper(event, EventPriority.NORMAL)
                    }
                    @EventHandler(priority = EventPriority.HIGH)
                    fun onChat3(event: io.papermc.paper.event.player.AsyncChatEvent) {
                        callPaper(event, EventPriority.HIGH)
                    }
                    @EventHandler(priority = EventPriority.HIGHEST)
                    fun onChat4(event: io.papermc.paper.event.player.AsyncChatEvent) {
                        callPaper(event, EventPriority.HIGHEST)
                    }
                }
            } catch (_: NoClassDefFoundError) {
                @Suppress("DEPRECATION")
                object : Listener {
                    fun callCb(event: org.bukkit.event.player.AsyncPlayerChatEvent, priority: EventPriority) {
                        val esu = callChatEvent(event, event.message.legacy, priority)
                        if (esu.changed) event.message = esu.message.legacy
                    }

                    @EventHandler(priority = EventPriority.LOWEST)
                    fun onChat0(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.LOWEST)
                    }
                    @EventHandler(priority = EventPriority.LOW)
                    fun onChat1(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.LOW)
                    }
                    @EventHandler(priority = EventPriority.NORMAL)
                    fun onChat2(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.NORMAL)
                    }
                    @EventHandler(priority = EventPriority.HIGH)
                    fun onChat3(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.HIGH)
                    }
                    @EventHandler(priority = EventPriority.HIGHEST)
                    fun onChat4(event: org.bukkit.event.player.AsyncPlayerChatEvent) {
                        callCb(event, EventPriority.HIGHEST)
                    }
                }
            }.register()
        }

    }

}