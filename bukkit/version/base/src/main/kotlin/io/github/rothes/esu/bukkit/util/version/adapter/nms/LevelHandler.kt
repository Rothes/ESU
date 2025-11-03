package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level

interface LevelHandler {

    fun level(player: ServerPlayer): ServerLevel
    fun level(entity: Entity): Level

}