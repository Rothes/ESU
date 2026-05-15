package io.github.rothes.esu.bukkit.event

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import org.bukkit.entity.Player
import org.bukkit.event.Event

abstract class EsuUserEvent(val player: Player, async: Boolean = false) : Event(async) {

    val user: PlayerUser by lazy {
        player.user
    }

}