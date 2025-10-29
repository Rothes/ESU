package io.github.rothes.esu.bukkit.util.version.adapter.nms.v1_21__paper

import io.github.rothes.esu.bukkit.util.version.adapter.nms.LevelEntitiesHandler
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity

class LevelEntitiesHandler: LevelEntitiesHandler {

    override fun getEntitiesAll(level: ServerLevel): Iterable<Entity> {
        return level.`moonrise$getEntityLookup`().all
    }

}