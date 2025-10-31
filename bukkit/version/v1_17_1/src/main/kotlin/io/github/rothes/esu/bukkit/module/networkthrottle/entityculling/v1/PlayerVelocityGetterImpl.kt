package io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.v1

import io.github.rothes.esu.bukkit.module.networkthrottle.entityculling.PlayerVelocityGetter
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3

class PlayerVelocityGetterImpl: PlayerVelocityGetter {

    override fun getPlayerMoveVelocity(player: ServerPlayer): Vec3 {
        return player.deltaMovement
    }

}