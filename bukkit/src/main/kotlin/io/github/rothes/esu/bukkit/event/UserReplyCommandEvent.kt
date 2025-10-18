package io.github.rothes.esu.bukkit.event

import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList

class UserReplyCommandEvent(
    player: Player,
    message: String,
    override var cancelledKt: Boolean,
    override val parentPriority: EventPriority,
): EsuUserEvent(player), CancellableKt, Nested {

    private var changed: Boolean = false

    var message: String = message
        set(value) {
            changed = true
            field = value
        }

    fun hasModified() = changed

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {
        const val REPLY_COMMANDS = "reply|r"

        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

}