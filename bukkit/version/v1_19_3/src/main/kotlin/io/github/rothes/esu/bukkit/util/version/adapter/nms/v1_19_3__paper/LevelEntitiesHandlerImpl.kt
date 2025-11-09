package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_19_3__paper

import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelEntitiesHandler
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.entity.LevelEntityGetter

class LevelEntitiesHandlerImpl: LevelEntitiesHandler {

    override fun getEntitiesAll(level: ServerLevel): Iterable<Entity> {
        return (level.entityLookup as LevelEntityGetter<Entity>).all // Convert to LevelEntityGetter to fix art re-obf
    }

}