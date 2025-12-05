package io.github.rothes.esu.bukkit.module.core

import org.bukkit.entity.Player

interface Provider {

    val isEnabled: Boolean

    fun lastMoveTime(player: Player): Long

}
