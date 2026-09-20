package io.github.rothes.esu.bukkit.module.anticheat.movement.v1

import io.github.rothes.esu.bukkit.module.anticheat.movement.ElytraStartGlideInterval
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer
import org.bukkit.entity.Player

object StopFlyingHandlerImpl : ElytraStartGlideInterval.StopFlyingHandler {

    override fun stopFallFlying(player: Player) {
        player as CraftPlayer
        val handle = player.handle
//        player.handle.stopFallFlying() // This callToggleGlideEvent, we don't like it, impl below

        // Entity.FLAG_FALL_FLYING = 7
        handle.setSharedFlag(7, true)
        handle.setSharedFlag(7, false)
    }

}