package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

interface PlayerEntityVisibilityHandler {

    fun showEntity(player: Player, bukkitEntity: Entity, plugin: Plugin = bootstrap) {
        try {
            player.showEntity(plugin, bukkitEntity)
        } catch (_: IllegalStateException) {
            // Entity.isVisibleByDefault() calls getHandle() which may check tickThread
            forceShowEntity(player, bukkitEntity, plugin)
        }
    }

    fun forceShowEntity(player: Player, bukkitEntity: Entity, plugin: Plugin = bootstrap)

}