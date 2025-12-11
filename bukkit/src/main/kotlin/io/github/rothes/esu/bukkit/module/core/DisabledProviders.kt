package io.github.rothes.esu.bukkit.module.core

import org.bukkit.entity.Player

object DisabledProviders: Providers {

    override val isEnabled: Boolean = false

    override val genericActiveTime = NowProvider
    override val attackTime = NowProvider
    override val posMoveTime = NowProvider
    override val moveTime = NowProvider

    object NowProvider: PlayerTimeProvider {

        override fun get(player: Player): Long = System.currentTimeMillis()

        override fun set(player: Player, time: Long) {}

        override fun registerListener(listener: PlayerTimeProvider.ChangeListener) {
            throw IllegalStateException("CoreModule is disabled")
        }
        override fun unregisterListener(listener: PlayerTimeProvider.ChangeListener) {}

    }

}