package io.github.rothes.esu.bukkit.util.version.adapter.nms

import io.github.rothes.esu.bukkit.util.version.adapter.TickThreadAdapter.Companion.checkTickThread
import io.github.rothes.esu.core.EsuBootstrap
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

interface PlayerEntityVisibilityHandler {

    fun showEntity(player: Player, bukkitEntity: Entity, plugin: Plugin = EsuBootstrap.instance as Plugin) {
        if (bukkitEntity.checkTickThread()) { // Entity.isVisibleByDefault() calls getHandle() which may check tickThread
            player.showEntity(plugin, bukkitEntity)
        } else {
            forceShowEntity(player, bukkitEntity, plugin)
        }
    }

    fun forceShowEntity(player: Player, bukkitEntity: Entity, plugin: Plugin = EsuBootstrap.instance as Plugin)

}