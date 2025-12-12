package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

interface PlayerEntityVisibilityHandler {

    fun forceShowEntity(player: Player, bukkitEntity: Entity, plugin: Plugin = bootstrap)

}