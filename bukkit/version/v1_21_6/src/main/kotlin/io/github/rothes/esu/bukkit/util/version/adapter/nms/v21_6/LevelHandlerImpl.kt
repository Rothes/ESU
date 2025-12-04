package io.github.rothes.esu.bukkit.util.version.adapter.nms.v21_6

import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelHandler
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level

class LevelHandlerImpl: LevelHandler {

    override fun level(player: ServerPlayer): ServerLevel {
        return player.level()
    }

    override fun level(entity: Entity): Level {
        return entity.level()
    }

}