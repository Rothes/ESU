package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import org.bukkit.entity.Entity
import org.bukkit.entity.Player

interface PlayerEntityVisibilityHandler {

    fun forceShowEntity(player: Player, bukkitEntity: Entity)

}