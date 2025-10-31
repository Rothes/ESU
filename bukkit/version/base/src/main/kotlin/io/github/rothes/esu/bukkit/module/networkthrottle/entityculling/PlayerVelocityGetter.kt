package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

interface PlayerVelocityGetter {

    fun getPlayerMoveVelocity(player: ServerPlayer): Vec3

}