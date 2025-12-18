package io.github.rothes.esu.bukkit.util.version.adapter.nms

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity

interface LevelEntitiesHandler {

    fun getEntitiesAll(level: ServerLevel): Iterable<Entity>

}