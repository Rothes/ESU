package io.github.rothes.esu.bukkit.module.core

import org.bukkit.entity.Player

object DisabledProvider: Provider {

    override val isEnabled: Boolean = false

    override fun lastMoveTime(player: Player): Long = System.currentTimeMillis()

}