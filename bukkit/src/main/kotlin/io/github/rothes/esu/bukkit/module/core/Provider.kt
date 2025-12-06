package io.github.rothes.esu.bukkit.module.core

import org.bukkit.entity.Player

interface Provider {

    val isEnabled: Boolean

    fun lastGenericActiveTime(player: Player): Long = maxOf(lastAttackTime(player), lastMoveTime(player))

    fun lastAttackTime(player: Player): Long
    fun lastMoveTime(player: Player): Long

}
