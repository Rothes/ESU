package io.github.rothes.esu.bukkit.module.core

interface Providers {

    val isEnabled: Boolean

    val genericActiveTime: PlayerTimeProvider

    val attackTime: PlayerTimeProvider
    val posMoveTime: PlayerTimeProvider
    val moveTime: PlayerTimeProvider

}
