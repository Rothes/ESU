package io.github.rothes.esu.bukkit.event

import io.github.rothes.esu.bukkit.user
import io.github.rothes.esu.bukkit.user.PlayerUser
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerEvent

abstract class EsuUserEvent(player: Player, async: Boolean = false) : PlayerEvent(player, async) {

    val user: PlayerUser by lazy {
        player.user
    }

}