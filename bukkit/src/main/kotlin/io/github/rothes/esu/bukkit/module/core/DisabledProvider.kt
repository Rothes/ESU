package io.github.rothes.esu.bukkit.module.core

import org.bukkit.entity.Player

object DisabledProvider: Provider {

    override val isEnabled: Boolean = false

    override fun lastGenericActiveTime(player: Player): Long = System.currentTimeMillis()

    override fun lastAttackTime(player: Player): Long = System.currentTimeMillis()
    override fun lastMoveTime(player: Player): Long = System.currentTimeMillis()

}