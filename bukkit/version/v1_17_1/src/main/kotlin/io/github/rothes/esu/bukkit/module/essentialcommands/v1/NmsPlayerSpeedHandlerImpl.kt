package io.github.rothes.esu.bukkit.module.essentialcommands.v1

import io.github.rothes.esu.bukkit.module.essentialcommands.Speed
import io.github.rothes.esu.bukkit.util.version.adapter.AttributeAdapter
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer
import org.bukkit.entity.Player as BukkitPlayer

object NmsPlayerSpeedHandlerImpl: Speed.NmsPlayerSpeedHandler {

    override fun getWalkSpeed(bukkitPlayer: BukkitPlayer) = bukkitPlayer.nms.abilities.walkingSpeed
    override fun getFlySpeed(bukkitPlayer: BukkitPlayer) = bukkitPlayer.nms.abilities.flyingSpeed

    override fun setWalkSpeed(bukkitPlayer: BukkitPlayer, speed: Float) {
        val player = bukkitPlayer.nms
        player.abilities.walkingSpeed = speed
        player.onUpdateAbilities()
        bukkitPlayer.getAttribute(AttributeAdapter.MOVEMENT_SPEED)!!.baseValue = speed.toDouble()
    }
    override fun setFlySpeed(bukkitPlayer: BukkitPlayer, speed: Float) {
        val player = bukkitPlayer.nms
        player.abilities.flyingSpeed = speed
        player.onUpdateAbilities()
    }

    private val BukkitPlayer.nms
        get() = (this as CraftPlayer).handle
}