package io.github.rothes.esu.bukkit.event

import io.github.rothes.esu.bukkit.user
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class UserTrackEntityEvent(
    player: Player,
    val entity: Entity
): PlayerEvent(player, false), CancellableKt {

    val user by lazy(LazyThreadSafetyMode.NONE) { player.user }

    override var cancelledKt: Boolean = false

    override fun getHandlers(): HandlerList = Companion.handlers

    companion object {

        private val handlers = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlers

    }

}