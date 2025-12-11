package io.github.rothes.esu.bukkit.module.core

import org.bukkit.entity.Player

interface PlayerTimeProvider {

    operator fun get(player: Player): Long
    operator fun set(player: Player, time: Long)

    fun registerListener(listener: ChangeListener)
    fun unregisterListener(listener: ChangeListener)

    interface ChangeListener {
        fun onTimeChanged(player: Player, oldTime: Long, newTime: Long)
    }

}