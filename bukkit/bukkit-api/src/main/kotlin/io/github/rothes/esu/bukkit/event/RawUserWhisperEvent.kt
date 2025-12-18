package io.github.rothes.esu.bukkit.event

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList

class RawUserWhisperEvent(
    player: Player,
    val target: String,
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

    val targetPlayer: Player? by lazy { Bukkit.getPlayer(target) }

    fun hasModified() = changed

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {
        const val WHISPER_COMMANDS = "whisper|msg|w|m|tell"

        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers
    }

}