package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import io.github.rothes.esu.bukkit.bootstrap
import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

interface PlayerEntityVisibilityHandler {

    fun showEntity(player: Player, bukkitEntity: Entity, plugin: Plugin = bootstrap) {
        if (bukkitEntity.checkTickThread()) { // Entity.isVisibleByDefault() calls getHandle() which may check tickThread
            player.showEntity(plugin, bukkitEntity)
        } else {
            forceShowEntity(player, bukkitEntity, plugin)
        }
    }

    fun forceShowEntity(player: Player, bukkitEntity: Entity, plugin: Plugin = bootstrap)

}